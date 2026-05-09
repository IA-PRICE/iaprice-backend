package ma.iaprice.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import ma.iaprice.backend.shared.BaseEntity;

/**
 * Représente un plan d'abonnement (free, starter, pro, elite).
 * Les plans sont insérés par Flyway — pas de création depuis le code.
 */
@Getter
@Entity
@Table(name = "plans")
public class Plan extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "price_mad", nullable = false)
    private java.math.BigDecimal priceMad;

    @Column(name = "product_limit", nullable = false)
    private int productLimit;

    @Column(name = "competitor_limit", nullable = false)
    private int competitorLimit;

    @Column(name = "search_limit", nullable = false)
    private int searchLimit;

    @Column(name = "has_alerts", nullable = false)
    private boolean hasAlerts;

    @Column(name = "has_reports", nullable = false)
    private boolean hasReports;

    @Column(name = "has_repricing", nullable = false)
    private boolean hasRepricing;

    @Column(name = "has_autopilot", nullable = false)
    private boolean hasAutopilot;

    @Column(name = "has_api", nullable = false)
    private boolean hasApi;
}
