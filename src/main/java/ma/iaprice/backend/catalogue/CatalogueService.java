package ma.iaprice.backend.catalogue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.iaprice.backend.entity.Product;
import ma.iaprice.backend.entity.Subscription;
import ma.iaprice.backend.repository.OrganizationRepository;
import ma.iaprice.backend.repository.ProductRepository;
import ma.iaprice.backend.repository.SubscriptionRepository;
import ma.iaprice.backend.security.TenantContext;
import ma.iaprice.backend.shared.exception.ProductNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service principal du Module 2 — Catalogue.
 * Responsabilités :
 *  - Lister les produits paginés avec filtres (US-CAT-01)
 *  - Calculer le quota du plan (US-CAT-01 CA-02)
 *  - Changer le statut d'un produit (US-CAT-02)
 * L'import/export est délégué à ImportExportService.
 * Sécurité multi-tenant : org_id extrait depuis TenantContext (JWT).
 * Un produit d'une autre org n'est JAMAIS visible (CA-06).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogueService {

    private final ProductRepository      productRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final OrganizationRepository organizationRepository;
    private final CatalogueMapper        mapper;

    // ── US-CAT-01 : Lister les produits ──────────────────────

    @Transactional(readOnly = true)
    public CatalogueDto.ProductPage listProducts(
            int page, int size, String sort,
            String search, Boolean isActive,
            String brand
    ) {
        UUID orgId   = TenantContext.getOrgId();
        Pageable pg  = buildPageable(page, size, sort);
        log.debug(">>> findByFilters search='{}' brand='{}' isActive={}", search, brand, isActive);
        Page<Product> productPage = productRepository.findByFilters(
                orgId, isActive, nullIfBlank(search), nullIfBlank(brand),
                pg
        );

        // Meta : totaux + dernier import
        long totalActive   = productRepository.countByOrganization_IdAndIsActiveTrue(orgId);
        long totalInactive = productRepository.countInactiveByOrgId(orgId);
        var  lastImported  = productRepository.findLastImportedAt(orgId).orElse(null);

        List<CatalogueDto.ProductDto> content = productPage.getContent()
                .stream().map(mapper::toDto).collect(Collectors.toList());

        return CatalogueDto.ProductPage.builder()
                .content(content)
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .meta(CatalogueDto.CatalogueMeta.builder()
                        .lastImportedAt(lastImported)
                        .totalActive(totalActive)
                        .totalInactive(totalInactive)
                        .build())
                .build();
    }

    // ── US-CAT-01 CA-02 : Quota ───────────────────────────────

    @Transactional(readOnly = true)
    public CatalogueDto.CatalogueQuota getQuota() {
        UUID orgId = TenantContext.getOrgId();

        var plan = subscriptionRepository
                .findByOrganization_IdAndStatus(orgId, "active")
                .map(Subscription::getPlan)
                .orElseThrow(() -> new IllegalStateException("Aucun abonnement actif trouvé."));

        long currentCount = productRepository.countByOrganization_IdAndIsActiveTrue(orgId);
        int  limit        = plan.getProductLimit();
        boolean unlimited = (limit == -1);

        long remaining   = unlimited ? -1 : Math.max(0, limit - currentCount);
        boolean canImport = unlimited || currentCount < limit;

        String upgradeMsg = null;
        if (!canImport) {
            upgradeMsg = String.format(
                    "Limite de %d produits atteinte (plan %s). Passez au plan Starter pour importer jusqu'à 100 produits.",
                    limit, capitalize(plan.getName())
            );
        }

        return CatalogueDto.CatalogueQuota.builder()
                .planName(plan.getName())
                .productLimit(limit)
                .currentProductCount(currentCount)
                .remainingSlots(remaining)
                .isUnlimited(unlimited)
                .canImport(canImport)
                .upgradeRequired(!canImport)
                .upgradeMessage(upgradeMsg)
                .build();
    }

    // ── US-CAT-02 : Changer le statut ────────────────────────

    @Transactional
    public CatalogueDto.ProductDto updateStatus(UUID productId, boolean isActive) {
        UUID orgId = TenantContext.getOrgId();

        Product product = productRepository
                .findByIdAndOrganization_Id(productId, orgId)
                .orElseThrow(ProductNotFoundException::new);

        product.setIsActive(isActive);
        product = productRepository.save(product);

        log.info("Statut produit {} → {} (org: {})", productId, isActive ? "actif" : "inactif", orgId);
        return mapper.toDto(product);
    }

    // ── Helpers privés ────────────────────────────────────────

    /**
     * Construit un Pageable à partir du paramètre sort "champ,direction".
     * Tri par défaut : name ASC (CA-05).
     */
    private Pageable buildPageable(int page, int size, String sort) {
        Sort sortObj = Sort.by("name").ascending(); // défaut CA-05
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String field = mapSortField(parts[0].trim());
            Sort.Direction dir = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()))
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            sortObj = Sort.by(dir, field);
        }
        return PageRequest.of(page, Math.min(size, 200), sortObj);
    }

    /** Mapping nom colonne API → nom champ JPA. */
    private String mapSortField(String apiField) {
        return switch (apiField.toLowerCase()) {
            case "name"      -> "name";
            case "brand"     -> "brand";
            case "myprice"   -> "myPrice";
            default          -> "name";
        };
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
