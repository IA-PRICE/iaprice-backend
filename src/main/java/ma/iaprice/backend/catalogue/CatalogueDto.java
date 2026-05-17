package ma.iaprice.backend.catalogue;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTOs du Module 2 — Catalogue.
 * Regroupés dans un seul fichier (cohérence avec AuthDto du Module 1).
 * Contrat API : openapi-catalogue-iaprice-v1.3.yaml
 */
public class CatalogueDto {

    // ══════════════════════════════════════════════════════════
    // RÉPONSES — Produit
    // ══════════════════════════════════════════════════════════

    /**
     * Représentation d'un produit dans les réponses API.
     * margin = calculée à la volée si cost != null, jamais stockée.
     */
    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProductDto {
        private UUID           id;
        private UUID           orgId;
        private String         name;
        private String         brand;
        private String         ean;
        private String         sku;
        private BigDecimal     price;
        private BigDecimal     cost;
        private Double         margin;        // calculée à la volée
        private String     pictureUrl;
        private String     productUrl;
        private boolean        isActive;
        private OffsetDateTime importedAt;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
    }

    // ══════════════════════════════════════════════════════════
    // RÉPONSES — Liste paginée
    // ══════════════════════════════════════════════════════════

    @Getter
    @Builder
    public static class ProductPage {
        private List<ProductDto>  content;
        private int               page;
        private int               size;
        private long              totalElements;
        private int               totalPages;
        private CatalogueMeta     meta;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CatalogueMeta {
        private OffsetDateTime lastImportedAt;
        private long           totalActive;
        private long           totalInactive;
    }

    // ══════════════════════════════════════════════════════════
    // RÉPONSES — Quota (US-CAT-01 CA-02)
    // ══════════════════════════════════════════════════════════

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CatalogueQuota {
        private String  planName;
        private int     productLimit;         // -1 = illimité
        private long    currentProductCount;
        private long    remainingSlots;       // -1 si illimité
        private boolean isUnlimited;
        private boolean canImport;
        private boolean upgradeRequired;
        private String  upgradeMessage;
    }

    // ══════════════════════════════════════════════════════════
    // RÉPONSES — Import (US-CAT-04)
    // ══════════════════════════════════════════════════════════

    @Getter
    @Builder
    public static class ImportResult {
        private int                  totalRows;
        private int                  createdCount;
        private int                  updatedCount;
        private int                  errorCount;
        private int                  priceChangesCount;
        private OffsetDateTime       importedAt;
        private List<ImportRowError> errors;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ImportRowError {
        private int    row;
        private String ean;
        private String sku;
        private String field;
        private String message;
    }

    // ══════════════════════════════════════════════════════════
    // REQUÊTES — Actions (US-CAT-02)
    // ══════════════════════════════════════════════════════════

    public record StatusRequest(boolean isActive) {}
}
