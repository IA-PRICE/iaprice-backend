package ma.iaprice.backend.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.iaprice.backend.config.JwtConfig;
import ma.iaprice.backend.entity.*;
import ma.iaprice.backend.auth.AuthDto.*;
import ma.iaprice.backend.repository.*;
import ma.iaprice.backend.security.JwtTokenProvider;
import ma.iaprice.backend.security.UserPrincipal;
import ma.iaprice.backend.shared.exception.EmailAlreadyExistsException;
import ma.iaprice.backend.shared.exception.InvalidCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository         userRepository;
    private final OrganizationRepository orgRepository;
    private final OrgMemberRepository    orgMemberRepository;
    private final PlanRepository         planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder        passwordEncoder;
    private final JwtTokenProvider       jwtTokenProvider;
    private final JwtConfig              jwtConfig;

    // ── REGISTER ──────────────────────────────────────────────
    @Transactional
    public AuthResponse register(RegisterRequest req) {

        // 1. Email unique
        if (userRepository.existsByEmail(req.email())) {
            throw new EmailAlreadyExistsException();
        }

        // 2. Créer le User
        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .firstName(req.firstName())
                .build();
        user = userRepository.save(user);

        // 3. Créer l'Organization
        Organization org = Organization.builder()
                .name(req.organizationName())
                .slug(generateSlug(req.organizationName()))
                .sector(req.sector() != null ? req.sector() : "autre")
                .build();
        org = orgRepository.save(org);

        // 4. Lier user → org (role = owner)
        OrgMember member = OrgMember.builder()
                .user(user)
                .organization(org)
                .role("owner")
                .build();
        orgMemberRepository.save(member);

        // 5. Créer la subscription Free (OBLIGATOIRE — QuotaGuard en semaine 6)
        Plan freePlan = planRepository.findByName("free")
                .orElseThrow(() -> new IllegalStateException("Plan 'free' introuvable en base."));

        Subscription subscription = Subscription.builder()
                .organization(org)
                .plan(freePlan)
                .status("active")
                .build();
        subscriptionRepository.save(subscription);

        log.info("Nouveau compte créé : {} / org: {}", user.getEmail(), org.getSlug());

        // 6. Générer le JWT et retourner la réponse
        String token = jwtTokenProvider.generateToken(user.getId(), org.getId(), "owner");
        return buildAuthResponse(token, user, org, freePlan);
    }

    // ── LOGIN ─────────────────────────────────────────────────
    @Transactional
    public AuthResponse login(LoginRequest req) {

        // 1. Vérifier l'email
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(InvalidCredentialsException::new);

        // 2. Vérifier le mot de passe
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        // 3. Récupérer l'org + le rôle
        OrgMember member = orgMemberRepository
                .findByUser_IdAndOrganization_Id(user.getId(), findOrgId(user))
                .orElseThrow(InvalidCredentialsException::new);

        Organization org = member.getOrganization();

        // 4. Récupérer le plan actif
        Plan plan = subscriptionRepository
                .findByOrganization_IdAndStatus(org.getId(), "active")
                .map(Subscription::getPlan)
                .orElseThrow(() -> new IllegalStateException("Aucun abonnement actif trouvé."));

        // 5. Mettre à jour lastLoginAt
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getId(), org.getId(), member.getRole());
        return buildAuthResponse(token, user, org, plan);
    }

    // ── ME ────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public MeResponse me() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable."));

        Organization org = orgRepository.findById(principal.getOrgId())
                .orElseThrow(() -> new IllegalStateException("Organisation introuvable."));

        Plan plan = subscriptionRepository
                .findByOrganization_IdAndStatus(org.getId(), "active")
                .map(Subscription::getPlan)
                .orElseThrow(() -> new IllegalStateException("Aucun abonnement actif trouvé."));

        return MeResponse.builder()
                .user(toUserDto(user))
                .organization(toOrgDto(org))
                .role(principal.getRole())
                .plan(plan.getName())
                .build();
    }

    // ── Helpers privés ────────────────────────────────────────

    private AuthResponse buildAuthResponse(String token, User user, Organization org, Plan plan) {
        return AuthResponse.builder()
                .token(token)
                .expiresIn(jwtConfig.getExpiration())
                .user(toUserDto(user))
                .organization(toOrgDto(org))
                .plan(plan.getName())
                .build();
    }

    private AuthDto.UserDto toUserDto(User user) {
        return AuthDto.UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .emailVerified(user.isEmailVerified())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    private AuthDto.OrgDto toOrgDto(Organization org) {
        return AuthDto.OrgDto.builder()
                .id(org.getId())
                .name(org.getName())
                .slug(org.getSlug())
                .sector(org.getSector())
                .currency(org.getCurrency())
                .build();
    }

    /**
     * Génère un slug URL-friendly depuis le nom de l'organisation.
     * ex: "Parapharmacie Atlas" → "parapharmacie-atlas"
     */
    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[àáâãäå]", "a")
                .replaceAll("[èéêë]", "e")
                .replaceAll("[ìíîï]", "i")
                .replaceAll("[òóôõö]", "o")
                .replaceAll("[ùúûü]", "u")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    /**
     * Trouve le premier org_id lié à un user (cas simple MVP : 1 user = 1 org).
     */
    private java.util.UUID findOrgId(User user) {
        return orgMemberRepository.findAll().stream()
                .filter(m -> m.getUser().getId().equals(user.getId()))
                .findFirst()
                .map(m -> m.getOrganization().getId())
                .orElseThrow(InvalidCredentialsException::new);
    }
}
