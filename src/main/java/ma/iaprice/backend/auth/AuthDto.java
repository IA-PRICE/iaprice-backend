package ma.iaprice.backend.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTOs du module Auth.
 * Regroupés dans un seul fichier pour garder le module compact (MVP).
 */
public class AuthDto {

    // ── Requêtes ──────────────────────────────────────────────

    public record RegisterRequest(
            @NotBlank @Email
            String email,

            @NotBlank @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères.")
            String password,

            @NotBlank
            String firstName,

            @NotBlank
            String organizationName,

            String sector  // optionnel
    ) {}

    public record LoginRequest(
            @NotBlank @Email
            String email,

            @NotBlank
            String password
    ) {}

    // ── Réponses ─────────────────────────────────────────────

    @Getter
    @Builder
    public static class AuthResponse {
        private String  token;
        private long    expiresIn;
        private UserDto user;
        private OrgDto  organization;
        private String  plan;
    }

    @Getter
    @Builder
    public static class MeResponse {
        private UserDto user;
        private OrgDto  organization;
        private String  role;
        private String  plan;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserDto {
        private UUID           id;
        private String         email;
        private String         firstName;
        private boolean        emailVerified;
        private OffsetDateTime lastLoginAt;
    }

    @Getter
    @Builder
    public static class OrgDto {
        private UUID   id;
        private String name;
        private String slug;
        private String sector;
        private String currency;
    }
}
