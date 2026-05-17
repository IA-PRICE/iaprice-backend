package ma.iaprice.backend.catalogue;

import ma.iaprice.backend.entity.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Mapper Manuel — Module 2 Catalogue.
 * Cohérence avec le projet (pas de MapStruct en dépendance).
 *
 * Règle marge :
 *   margin = ((Price - cost) / myPrice) × 100
 *   Retourné null si cost est null ou myPrice est 0.
 *   Jamais stocké en base (readOnly dans l'OpenAPI).
 */
@Component
public class CatalogueMapper {

    public CatalogueDto.ProductDto toDto(Product p) {
        return CatalogueDto.ProductDto.builder()
                .id(p.getId())
                .orgId(p.getOrganization().getId())
                .name(p.getName())
                .brand(p.getBrand())
                .ean(p.getEan())
                .sku(p.getSku())
                .price(p.getPrice())
                .cost(p.getCost())
                .margin(computeMargin(p.getPrice(), p.getCost()))
                .isActive(p.getIsActive())
                .importedAt(p.getImportedAt())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────

    private Double computeMargin(BigDecimal myPrice, BigDecimal cost) {
        if (cost == null || myPrice == null || myPrice.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return myPrice.subtract(cost)
                .divide(myPrice, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
