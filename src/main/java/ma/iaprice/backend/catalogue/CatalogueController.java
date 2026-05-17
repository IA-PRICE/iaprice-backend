package ma.iaprice.backend.catalogue;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Controller REST — Module 2 Catalogue.
 *
 * Endpoints :
 *   GET  /catalogue/products               → US-CAT-01
 *   GET  /catalogue/quota                  → US-CAT-01 CA-02
 *   PATCH /catalogue/products/{id}/status  → US-CAT-02
 *   GET  /catalogue/import/template        → US-CAT-04 CA-02
 *   POST /catalogue/import                 → US-CAT-04
 *   GET  /catalogue/export                 → US-CAT-05
 *
 * Sécurité : JWT requis sur tous les endpoints (SecurityConfig).
 * org_id résolu via TenantContext (injecté par JwtAuthFilter).
 */
@RestController
@RequestMapping("/catalogue")
@RequiredArgsConstructor
@Tag(name = "Catalogue", description = "Gestion du catalogue produits")
@SecurityRequirement(name = "BearerAuth")
public class CatalogueController {

    private final CatalogueService    catalogueService;
    private final ImportExportService importExportService;

    // ── US-CAT-01 : Liste paginée ─────────────────────────────

    @GetMapping("/products")
    @Operation(summary = "Lister les produits du catalogue")
    public ResponseEntity<CatalogueDto.ProductPage> listProducts(
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "10")   int size,
            @RequestParam(required = false)      String sort,
            @RequestParam(required = false)      String search,
            @RequestParam(required = false)      Boolean isActive,
            @RequestParam(required = false)      String brand
    ) {
        return ResponseEntity.ok(catalogueService.listProducts(
                page, size, sort, search, isActive, brand
        ));
    }

    // ── US-CAT-01 CA-02 : Quota ───────────────────────────────

    @GetMapping("/quota")
    @Operation(summary = "Consulter le quota produits du plan actif")
    public ResponseEntity<CatalogueDto.CatalogueQuota> getQuota() {
        return ResponseEntity.ok(catalogueService.getQuota());
    }

    // ── US-CAT-02 : Changer le statut ────────────────────────

    @PatchMapping("/products/{productId}/status")
    @Operation(summary = "Activer ou désactiver un produit")
    public ResponseEntity<CatalogueDto.ProductDto> updateStatus(
            @PathVariable UUID productId,
            @RequestBody   CatalogueDto.StatusRequest request
    ) {
        return ResponseEntity.ok(catalogueService.updateStatus(productId, request.isActive()));
    }

    // ── US-CAT-04 CA-02 : Template CSV ───────────────────────

    @GetMapping("/import/template")
    @Operation(summary = "Télécharger le template CSV officiel")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] content = importExportService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"catalogue_template.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(content);
    }

    // ── US-CAT-04 : Import CSV ────────────────────────────────

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importer le catalogue via un fichier CSV")
    public ResponseEntity<CatalogueDto.ImportResult> importCsv(
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(importExportService.importCsv(file));
    }

    // ── US-CAT-05 : Export ────────────────────────────────────

    @GetMapping("/export")
    @Operation(summary = "Exporter le catalogue en CSV ou Excel")
    public ResponseEntity<byte[]> exportCatalogue(
            @RequestParam(defaultValue = "CSV") String format,
            @RequestParam(required = false)     Boolean isActive,
            @RequestParam(required = false)     String search,
            @RequestParam(required = false)     String brand
    ) {
        String filename = "catalogue_export_" + LocalDate.now() + ".csv";
        byte[] content = importExportService.exportCsv(isActive, search, brand);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(content);
    }
}
