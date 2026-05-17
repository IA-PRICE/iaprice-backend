package ma.iaprice.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.iaprice.backend.shared.BaseEntity;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Entité principale du Module 2 — Catalogue.
 * Table : products
 * STOCK : 2 valeurs uniquement → in_stock / out_of_stock
 * Marge : calculée à la volée (jamais stockée en base).
 * Upsert : par EAN (prioritaire) puis SKU.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "products", indexes = {
        @Index(name = "idx_products_org_id",     columnList = "org_id"),
        @Index(name = "idx_products_ean",         columnList = "ean"),
        @Index(name = "idx_products_sku",         columnList = "sku"),
        @Index(name = "idx_products_is_active",   columnList = "is_active"),
        @Index(name = "idx_products_imported_at", columnList = "imported_at")
})
public class Product extends BaseEntity {

    // ── Tenant ────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    // ── Identification ────────────────────────────────────────
    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 100)
    private String brand;

    /** EAN-13 ou autre code-barres (optionnel, unique par org). */
    @Column(length = 50)
    private String ean;

    /** Référence interne (optionnel, unique par org). */
    @Column(length = 100)
    private String sku;

    // ── Tarification ─────────────────────────────────────────
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(precision = 10, scale = 2)
    private BigDecimal cost;

    // ── Statut ────────────────────────────────────────────────
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "picture_url", length = 500)
    private String pictureUrl;

    @Column(name = "product_url", length = 500)
    private String productUrl;

    // ── Horodatage import ─────────────────────────────────────
    /** Alimenté par le service Import. Sert pour meta.lastImportedAt. */
    @Column(name = "imported_at")
    private OffsetDateTime importedAt;

    // ── Timestamps ────────────────────────────────────────────
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
