package ma.iaprice.backend.catalogue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.iaprice.backend.entity.MyPriceHistory;
import ma.iaprice.backend.entity.Organization;
import ma.iaprice.backend.entity.Product;
import ma.iaprice.backend.entity.Subscription;
import ma.iaprice.backend.repository.MyPriceHistoryRepository;
import ma.iaprice.backend.repository.OrganizationRepository;
import ma.iaprice.backend.repository.ProductRepository;
import ma.iaprice.backend.repository.SubscriptionRepository;
import ma.iaprice.backend.security.TenantContext;
import ma.iaprice.backend.shared.exception.CatalogueImportException;
import ma.iaprice.backend.shared.exception.QuotaExceededException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Service Import/Export — Module 2 Catalogue.
 *
 * US-CAT-04 : Import CSV synchrone
 *   - Séparateur imposé : point-virgule (;)
 *   - Stratégie : upsert par EAN (prioritaire) puis SKU
 *   - Si my_price change → INSERT my_price_history (change_source=import)
 *   - Quota : COUNT(is_active=true) + nouveaux inserts <= product_limit
 *
 * US-CAT-05 : Export CSV (XLSX non implémenté MVP — retourne 501)
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportExportService {

    private static final String   SEPARATOR = ";";
    private static final long     MAX_SIZE  = 10L * 1024 * 1024; // 10 MB

    // Colonnes obligatoires (CA-03 US-CAT-04)
    private static final List<String> REQUIRED_COLS  = List.of("name", "price");
    // Toutes les colonnes du template (CA-02 US-CAT-04)
    private static final List<String> TEMPLATE_COLS  = List.of(
            "sku", "ean", "name", "brand", "picture_url", "product_url", "price", "cost"
    );

    private final ProductRepository      productRepository;
    private final MyPriceHistoryRepository priceHistoryRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final OrganizationRepository organizationRepository;

    // ── Template CSV (US-CAT-04 CA-02) ───────────────────────

    public byte[] generateTemplate() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(SEPARATOR, TEMPLATE_COLS)).append("\n");

        // 6 produits réels
        sb.append("3518646128103;REF-001;Gilbert Liniment Oleo-Calcaire 1L;Gilbert;https://www.gilbert-pharma.fr/img/liniment-1l.jpg;https://www.monstore.ma/gilbert-liniment-1l;154.00;90.00\n");
        sb.append("3600542469807;REF-002;L'Oreal Men Expert Hydra Power Gel 50ml;L'Oreal;https://www.loreal-paris.fr/img/hydra-power.jpg;https://www.monstore.ma/loreal-hydra-power;89.00;45.00\n");
        sb.append("8720689011662;REF-003;Philips Avent Biberon Natural 260ml;Philips Avent;https://www.philips.com/img/avent-biberon-260.jpg;https://www.monstore.ma/philips-avent-biberon;149.00;85.00\n");
        sb.append("3614272051608;REF-004;Vichy Dercos Shampooing Anti-Pelliculaire 200ml;Vichy;https://www.vichy.fr/img/dercos-anti-p.jpg;https://www.monstore.ma/vichy-dercos-200ml;119.00;65.00\n");
        sb.append("3401597480676;REF-005;Doliprane 1000mg 8 comprimés;Sanofi;https://www.sanofi.fr/img/doliprane-1000.jpg;https://www.monstore.ma/doliprane-1000mg;32.00;18.00\n");
        sb.append("6111245010491;REF-006;Huile d'Argan Bio Presse a Froid 100ml;Zineglob;https://www.zineglob.ma/img/huile-argan-100ml.jpg;https://www.monstore.ma/huile-argan-bio-100ml;185.00;95.00\n");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ── Import CSV (US-CAT-04) ────────────────────────────────

    @Transactional
    public CatalogueDto.ImportResult importCsv(MultipartFile file) {
        UUID orgId = TenantContext.getOrgId();

        // Validation fichier
        validateFile(file);

        List<String[]> rows = parseCsv(file);
        if (rows.isEmpty()) {
            throw new CatalogueImportException("EMPTY_FILE", "Le fichier CSV est vide.", null);
        }

        // En-têtes
        String[] headers = rows.get(0);
        Map<String, Integer> colIndex = buildColumnIndex(headers);
        validateRequiredColumns(colIndex);

        // Organisation (pour lier les produits)
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalStateException("Organisation introuvable."));

        // Plan actif → quota
        var plan = subscriptionRepository
                .findByOrganization_IdAndStatus(orgId, "active")
                .map(Subscription::getPlan)
                .orElseThrow(() -> new IllegalStateException("Aucun abonnement actif."));

        long currentCount = productRepository.countByOrganization_IdAndIsActiveTrue(orgId);
        int  productLimit = plan.getProductLimit();

        // Traitement ligne par ligne
        int created = 0, updated = 0, errors = 0, priceChanges = 0;
        List<CatalogueDto.ImportRowError> errorList = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();

        // Pré-calcul des nouveaux inserts pour vérification quota avant traitement
        int potentialInserts = countPotentialInserts(rows, colIndex, orgId);
        if (productLimit != -1 && (currentCount + potentialInserts) > productLimit) {
            throw new QuotaExceededException(potentialInserts, plan.getName(), productLimit);
        }

        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            int rowNum = i + 1; // 1-indexed pour l'utilisateur

            try {
                ProcessResult result = processRow(row, colIndex, org, now);
                if (result.isCreated())      created++;
                else                         updated++;
                if (result.isPriceChanged()) priceChanges++;

            } catch (RowValidationException ex) {
                errors++;
                errorList.add(CatalogueDto.ImportRowError.builder()
                        .row(rowNum)
                        .ean(safeGet(row, colIndex, "ean"))
                        .sku(safeGet(row, colIndex, "sku"))
                        .field(ex.getField())
                        .message(ex.getMessage())
                        .build());
            }
        }

        log.info("Import CSV org={} : créés={} mis_à_jour={} erreurs={}", orgId, created, updated, errors);

        return CatalogueDto.ImportResult.builder()
                .totalRows(rows.size() - 1)
                .createdCount(created)
                .updatedCount(updated)
                .errorCount(errors)
                .priceChangesCount(priceChanges)
                .importedAt(now)
                .errors(errorList)
                .build();
    }

    // ── Export CSV (US-CAT-05) ────────────────────────────────

    public byte[] exportCsv(Boolean isActive, String search, String brand) {
        UUID orgId = TenantContext.getOrgId();

        List<Product> products = productRepository.findForExport(
                orgId,
                isActive,
                nullIfBlank(search),
                nullIfBlank(brand)
        );

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(SEPARATOR, TEMPLATE_COLS)).append("\n");

        for (Product p : products) {
            sb.append(safe(p.getSku())).append(SEPARATOR)
                    .append(safe(p.getEan())).append(SEPARATOR)
                    .append(safe(p.getName())).append(SEPARATOR)
                    .append(safe(p.getBrand())).append(SEPARATOR)
                    .append(safe(p.getPictureUrl())).append(SEPARATOR)
                    .append(safe(p.getProductUrl())).append(SEPARATOR)
                    .append(p.getPrice() != null ? p.getPrice().toPlainString() : "").append(SEPARATOR)
                    .append(p.getCost()  != null ? p.getCost().toPlainString()  : "")
                    .append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ── Helpers privés ────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CatalogueImportException("EMPTY_FILE", "Le fichier est vide.", null);
        }
        if (file.getSize() > MAX_SIZE) {
            throw new CatalogueImportException("FILE_TOO_LARGE",
                    "Le fichier dépasse la limite de 10 MB.", null);
        }
        String ct = file.getContentType();
        if (ct != null && !ct.contains("csv") && !ct.contains("text")) {
            throw new CatalogueImportException("INVALID_FORMAT",
                    "Le fichier doit être au format CSV (UTF-8).", null);
        }
    }

    private List<String[]> parseCsv(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<String[]> rows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    rows.add(line.split(SEPARATOR, -1));
                }
            }
            return rows;
        } catch (IOException e) {
            throw new CatalogueImportException("ENCODING_ERROR",
                    "Impossible de lire le fichier. Vérifiez l'encodage UTF-8.", null);
        }
    }

    private Map<String, Integer> buildColumnIndex(String[] headers) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            index.put(headers[i].trim().toLowerCase().replace("\"", ""), i);
        }
        return index;
    }

    private void validateRequiredColumns(Map<String, Integer> colIndex) {
        List<String> missing = REQUIRED_COLS.stream()
                .filter(col -> !colIndex.containsKey(col))
                .toList();
        if (!missing.isEmpty()) {
            throw new CatalogueImportException(
                    "MISSING_REQUIRED_COLUMN",
                    "Colonnes obligatoires manquantes.",
                    missing
            );
        }
    }

    /**
     * Pré-calcule le nombre de lignes qui seront des INSERT (pas d'upsert existant).
     * Utilisé pour la vérification de quota AVANT le traitement réel.
     */
    private int countPotentialInserts(List<String[]> rows, Map<String, Integer> colIndex, UUID orgId) {
        int count = 0;
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            String ean = safeGet(row, colIndex, "ean");
            // Convertir la notation scientifique en entier string
            if (ean != null) {
                try {
                    ean = new BigDecimal(ean).toBigIntegerExact().toString();
                } catch (Exception e) {
                    // pas un nombre, garder tel quel
                }
            }
            String sku = safeGet(row, colIndex, "sku");

            boolean exists = false;
            if (ean != null && !ean.isBlank()) {
                exists = productRepository.findByOrganization_IdAndEan(orgId, ean).isPresent();
            }
            if (!exists && sku != null && !sku.isBlank()) {
                exists = productRepository.findByOrganization_IdAndSku(orgId, sku).isPresent();
            }
            if (!exists) count++;
        }
        return count;
    }

    /**
     * Traite une ligne CSV : upsert par EAN puis SKU.
     * Lance RowValidationException si la ligne est invalide.
     */
    private ProcessResult processRow(String[] row, Map<String, Integer> colIndex,
                                     Organization org, OffsetDateTime now) {
        // ── name (obligatoire) ────────────────────────────────
        String name = safeGet(row, colIndex, "name");
        if (name == null || name.isBlank()) {
            throw new RowValidationException("name", "Le nom est obligatoire.");
        }

        // ── price (obligatoire) ────────────────────────────
        String myPriceStr = safeGet(row, colIndex, "price");
        BigDecimal myPrice;
        try {
            myPrice = new BigDecimal(myPriceStr.replace(",", "."));
            if (myPrice.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
        } catch (NumberFormatException | NullPointerException e) {
            throw new RowValidationException("my_price", "Valeur invalide : doit être un nombre positif.");
        }

        BigDecimal cost       = parseBigDecimal(safeGet(row, colIndex, "cost"));
        String     pictureUrl = safeGet(row, colIndex, "picture_url");
        String     productUrl = safeGet(row, colIndex, "product_url");

        String ean  = safeGet(row, colIndex, "ean");
        String sku  = safeGet(row, colIndex, "sku");

        // ── Upsert : recherche par EAN puis SKU ───────────────
        Optional<Product> existing = Optional.empty();
        if (ean != null && !ean.isBlank()) {
            existing = productRepository.findByOrganization_IdAndEan(org.getId(), ean);
        }
        if (existing.isEmpty() && sku != null && !sku.isBlank()) {
            existing = productRepository.findByOrganization_IdAndSku(org.getId(), sku);
        }

        String     brand    = safeGet(row, colIndex, "brand");

        boolean created      = false;
        boolean priceChanged = false;

        if (existing.isPresent()) {
            // ── UPDATE ────────────────────────────────────────
            Product p = existing.get();

            // Détecter un changement de prix
            if (p.getPrice().compareTo(myPrice) != 0) {
                priceHistoryRepository.save(MyPriceHistory.builder()
                        .product(p)
                        .oldPrice(p.getPrice())
                        .newPrice(myPrice)
                        .changeSource("import")
                        .build());
                priceChanged = true;
            }

            p.setName(name);
            p.setPrice(myPrice);
            p.setIsActive(true); // réactiver si inactif (spec import)
            if (brand    != null) p.setBrand(brand);
            if (pictureUrl    != null) p.setPictureUrl(pictureUrl);
            if (productUrl    != null) p.setProductUrl(productUrl);
            if (ean      != null) p.setEan(ean);
            if (sku      != null) p.setSku(sku);
            if (cost     != null) p.setCost(cost);

            p.setImportedAt(now);

            productRepository.save(p);

        } else {
            // ── INSERT ────────────────────────────────────────
            Product p = Product.builder()
                    .organization(org)
                    .name(name)
                    .ean(nullIfBlank(ean))
                    .sku(nullIfBlank(sku))
                    .brand(nullIfBlank(brand))
                    .price(myPrice)
                    .cost(cost)
                    .pictureUrl(nullIfBlank(pictureUrl))
                    .productUrl(nullIfBlank(productUrl))
                    .isActive(true)
                    .importedAt(now)
                    .build();
            productRepository.save(p);
            created = true;
        }

        return new ProcessResult(created, priceChanged);
    }

    // ── Micro-helpers ─────────────────────────────────────────

    private String safeGet(String[] row, Map<String, Integer> colIndex, String col) {
        Integer idx = colIndex.get(col);
        if (idx == null || idx >= row.length) return null;
        String val = row[idx].trim();
        return val.isEmpty() ? null : val;
    }

    private BigDecimal parseBigDecimal(String s) {
        if (s == null || s.isBlank()) return null;
        try { return new BigDecimal(s.replace(",", ".")); }
        catch (NumberFormatException e) { return null; }
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private String safe(String s) {
        return s == null ? "" : s.replace(SEPARATOR, " ");
    }

    // ── Classes internes ──────────────────────────────────────

    private record ProcessResult(boolean isCreated, boolean isPriceChanged) {}

    /** Exception interne pour signaler une erreur sur une ligne CSV. */
    static class RowValidationException extends RuntimeException {
        private final String field;
        public RowValidationException(String field, String message) {
            super(message);
            this.field = field;
        }
        public String getField() { return field; }
    }
}
