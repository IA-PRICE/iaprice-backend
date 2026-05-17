package ma.iaprice.backend.shared;

import ma.iaprice.backend.shared.exception.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestion centralisée des erreurs — Module 1 + Module 2.
 * Format uniforme : { "code": "...", "message": "...", [champs extra] }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Module 1 — Auth ───────────────────────────────────────

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailExists(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error("EMAIL_ALREADY_EXISTS", ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(error("INVALID_CREDENTIALS", ex.getMessage()));
    }

    // ── Module 2 — Catalogue ──────────────────────────────────

    /** 404 — Produit introuvable (US-CAT-02 CA-04) */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProductNotFound(ProductNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error("PRODUCT_NOT_FOUND", ex.getMessage()));
    }

    /** 400 — Fichier CSV invalide avant traitement (US-CAT-04 CA-05) */
    @ExceptionHandler(CatalogueImportException.class)
    public ResponseEntity<Map<String, Object>> handleImportError(CatalogueImportException ex) {
        Map<String, Object> body = error(ex.getCode(), ex.getMessage());
        if (ex.getMissingColumns() != null && !ex.getMissingColumns().isEmpty()) {
            body.put("missingColumns", ex.getMissingColumns());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /** 402 — Quota dépassé (US-CAT-04 CA-05) */
    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<Map<String, Object>> handleQuotaExceeded(QuotaExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(error("QUOTA_EXCEEDED", ex.getMessage()));
    }

    // ── Validation @Valid ─────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Données invalides.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error("VALIDATION_ERROR", message));
    }

    // ── 500 : Catch-all ─────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error("INTERNAL_ERROR", "Une erreur inattendue s'est produite."));
    }

    // ── Helper ───────────────────────────────────────────────

    private Map<String, Object> error(String code, String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("message", message);
        return map;
    }

    // ── 409 : contrainte unique BDD (filet de sécurité) ─────────
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrity(DataIntegrityViolationException e) {
        return ResponseEntity.status(409).body(Map.of(
                "message", "Une ressource avec ces informations existe déjà.",
                "code", "CONFLICT"
        ));
    }
}
