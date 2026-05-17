package ma.iaprice.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.iaprice.backend.entity.User;
import ma.iaprice.backend.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtre exécuté une fois par requête.
 * 1. Extrait le token du header Authorization: Bearer <token>
 * 2. Valide le token
 * 3. Alimente le SecurityContext (Spring Security)
 * 4. Alimente le TenantContext (org_id pour les services)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository   userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);

            if (StringUtils.hasText(token) && jwtTokenProvider.isValid(token)) {
                UUID userId = jwtTokenProvider.getUserId(token);
                UUID orgId  = jwtTokenProvider.getOrgId(token);
                String role = jwtTokenProvider.getRole(token);

                User user = userRepository.findById(userId).orElse(null);
                if (user != null && user.getIsActive()) {
                    UserPrincipal principal = new UserPrincipal(user, orgId, role);

                    // Spring Security
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    // Multi-tenant context
                    TenantContext.setOrgId(orgId);
                }
            }
        } catch (Exception e) {
            log.error("Erreur dans JwtAuthFilter : {}", e.getMessage());
        } finally {
            filterChain.doFilter(request, response);
            TenantContext.clear(); // TOUJOURS nettoyer après la requête
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
