package ma.iaprice.backend.repository;

import ma.iaprice.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository du Module 2 — Catalogue.
 * Requêtes nommées suffixent avec AND p.organization.id = :orgId
 * pour garantir l'isolation multi-tenant (CA-06 US-CAT-01).
 */
public interface ProductRepository extends JpaRepository<Product, UUID> {

    // ── Recherche principale (US-CAT-01) ─────────────────────

    /**
     * Recherche paginée avec filtres optionnels.
     * - isActive null → tous les produits (filtre "Tous")
     * - search null   → pas de filtre texte
     * - stockStatus null → pas de filtre stock
     * - myPriceMin / myPriceMax null → pas de filtre prix
     */
    @Query("""
    SELECT p FROM Product p
    WHERE p.organization.id = :orgId
      AND (:isActive IS NULL OR p.isActive = :isActive)
      AND (COALESCE(:search, '') = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
      AND (COALESCE(:brand, '') = '' OR LOWER(p.brand) = LOWER(:brand))
""")
    Page<Product> findByFilters(
            @Param("orgId")        UUID orgId,
            @Param("isActive")     Boolean isActive,
            @Param("search")       String search,
            @Param("brand")        String brand,
            Pageable pageable
    );

    // ── Quota  ────────────

    /** Compte les produits actifs → utilisé pour le quota. */
    long countByOrganization_IdAndIsActiveTrue(UUID orgId);

    // ── Meta (lastImportedAt) ─────────────────────────────────

    @Query("SELECT MAX(p.importedAt) FROM Product p WHERE p.organization.id = :orgId")
    Optional<OffsetDateTime> findLastImportedAt(@Param("orgId") UUID orgId);


    @Query("SELECT COUNT(p) FROM Product p WHERE p.organization.id = :orgId AND p.isActive = false")
    long countInactiveByOrgId(@Param("orgId") UUID orgId);

    // ── Upsert import ─────────────────────────────

    Optional<Product> findByOrganization_IdAndEan(UUID orgId, String ean);

    Optional<Product> findByOrganization_IdAndSku(UUID orgId, String sku);

    // ── Sécurité : vérification tenant ───────────

    Optional<Product> findByIdAndOrganization_Id(UUID id, UUID orgId);

    // ── Export (US-CAT-05) ────────────────────────────────────

    @Query("""
        SELECT p FROM Product p
        WHERE p.organization.id = :orgId
          AND (:isActive IS NULL OR p.isActive = :isActive)
          AND (:search IS NULL OR
               LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(p.ean)  LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(p.sku)  LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:brand IS NULL OR LOWER(p.brand) = LOWER(:brand))
        ORDER BY p.name ASC
    """)
    java.util.List<Product> findForExport(
            @Param("orgId")       UUID orgId,
            @Param("isActive")    Boolean isActive,
            @Param("search")      String search,
            @Param("brand")       String brand
    );
}
