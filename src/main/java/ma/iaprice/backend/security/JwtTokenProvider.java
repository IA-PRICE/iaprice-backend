package ma.iaprice.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.iaprice.backend.config.JwtConfig;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Génère et valide les JWT.
 *
 * Claims stockés dans le token :
 *   - sub     : userId (UUID)
 *   - orgId   : UUID de l'organisation
 *   - role    : rôle dans l'organisation (owner, admin, viewer)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;

    // ── Génération ────────────────────────────────────────────
    public String generateToken(UUID userId, UUID orgId, String role) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + jwtConfig.getExpiration() * 1000);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("orgId", orgId.toString())
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getKey())
                .compact();
    }

    // ── Extraction des claims ─────────────────────────────────
    public UUID getUserId(String token) {
        return UUID.fromString(getClaims(token).getSubject());
    }

    public UUID getOrgId(String token) {
        return UUID.fromString(getClaims(token).get("orgId", String.class));
    }

    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    // ── Validation ────────────────────────────────────────────
    public boolean isValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT expiré");
        } catch (JwtException e) {
            log.debug("JWT invalide : {}", e.getMessage());
        }
        return false;
    }

    // ── Helpers privés ────────────────────────────────────────
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
