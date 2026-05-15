package ma.iaprice.backend.shared;

import ma.iaprice.backend.shared.exception.EmailAlreadyExistsException;
import ma.iaprice.backend.shared.exception.InvalidCredentialsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Gestion centralisée des erreurs.
 * Retourne toujours : { "code": "...", "message": "..." }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 409 : email déjà utilisé ─────────────────────────────
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleEmailExists(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error("EMAIL_ALREADY_EXISTS", ex.getMessage()));
    }

    // ── 401 : mauvais identifiants ───────────────────────────
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(error("INVALID_CREDENTIALS", ex.getMessage()));
    }

    // ── 400 : erreurs de validation (@Valid) ─────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Données invalides.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error("VALIDATION_ERROR", message));
    }

    // ── 500 : erreurs inattendues ────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        // En prod, ne pas exposer ex.getMessage() — logger à la place
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error("INTERNAL_ERROR", "Une erreur inattendue s'est produite."));
    }

    // ── Helper ───────────────────────────────────────────────
    private Map<String, String> error(String code, String message) {
        return Map.of("code", code, "message", message);
    }

    // ── 409 : contrainte unique BDD (filet de sécurité) ─────────
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error("CONFLICT", "Une ressource avec ces informations existe déjà."));
    }
}
