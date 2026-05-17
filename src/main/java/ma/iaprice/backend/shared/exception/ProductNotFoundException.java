package ma.iaprice.backend.shared.exception;

/**
 * Lancée quand un produit est introuvable ou n'appartient pas à l'org.
 * → HTTP 404 avec code PRODUCT_NOT_FOUND (US-CAT-02 CA-04)
 */
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException() {
        super("Aucun produit trouvé pour cet identifiant.");
    }
}
