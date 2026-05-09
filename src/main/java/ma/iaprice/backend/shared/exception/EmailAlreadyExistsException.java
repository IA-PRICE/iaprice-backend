package ma.iaprice.backend.shared.exception;

// ── EMAIL_ALREADY_EXISTS ─────────────────────────────────────
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException() {
        super("Cet email est déjà associé à un compte.");
    }
}
