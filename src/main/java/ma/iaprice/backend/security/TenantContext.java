package ma.iaprice.backend.security;

import java.util.UUID;

/**
 * Stocke l'org_id de la requête courante dans un ThreadLocal.
 * Alimenté par JwtAuthFilter, disponible partout dans les services.
 *
 * IMPORTANT : toujours appeler clear() en fin de requête (fait dans JwtAuthFilter).
 */
public class TenantContext {

    private static final ThreadLocal<UUID> currentOrgId = new ThreadLocal<>();

    public static void setOrgId(UUID orgId) {
        currentOrgId.set(orgId);
    }

    public static UUID getOrgId() {
        return currentOrgId.get();
    }

    public static void clear() {
        currentOrgId.remove();
    }
}
