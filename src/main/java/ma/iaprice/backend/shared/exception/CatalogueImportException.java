package ma.iaprice.backend.shared.exception;

import lombok.Getter;

import java.util.List;

/**
 * Lancée pour les erreurs de validation du fichier CSV AVANT traitement.
 * → HTTP 400 avec codes : MISSING_REQUIRED_COLUMN | INVALID_FORMAT |
 *   ENCODING_ERROR | FILE_TOO_LARGE | EMPTY_FILE

 * (US-CAT-04 CA-05)
 */
@Getter
public class CatalogueImportException extends RuntimeException {
    private final String       code;
    private final List<String> missingColumns;

    public CatalogueImportException(String code, String message, List<String> missingColumns) {
        super(message);
        this.code           = code;
        this.missingColumns = missingColumns;
    }
}
