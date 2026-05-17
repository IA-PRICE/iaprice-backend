package ma.iaprice.backend.shared.exception;

import lombok.Getter;

/**
 * Lancée quand l'import dépasserait le quota du plan.
 * → HTTP 402 avec code QUOTA_EXCEEDED (US-CAT-04 CA-05)
 */
@Getter
public class QuotaExceededException extends RuntimeException {
    private final int    newProducts;
    private final String planName;
    private final int    limit;

    public QuotaExceededException(int newProducts, String planName, int limit) {
        super(String.format(
                "Ce fichier ajouterait %d produits mais votre plan %s est limité à %d.",
                newProducts, capitalize(planName), limit
        ));
        this.newProducts = newProducts;
        this.planName    = planName;
        this.limit       = limit;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
