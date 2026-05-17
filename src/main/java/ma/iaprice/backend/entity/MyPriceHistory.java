package ma.iaprice.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.iaprice.backend.shared.BaseEntity;

import java.math.BigDecimal;

/**
 * Historique des changements de prix (my_price).
 * Table : my_price_history

 * Un enregistrement est inséré automatiquement chaque fois que
 * my_price change lors d'un import CSV (change_source = "import").
 * Prévu pour être étendu (autopilot, manuel…) dans les modules suivants.
 */
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "my_price_history", indexes = {
        @Index(name = "idx_price_history_product_id", columnList = "product_id")
})
public class MyPriceHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "old_price", precision = 10, scale = 2)
    private BigDecimal oldPrice;

    @Column(name = "new_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal newPrice;

    /**
     * Source du changement : "import" | "manual" | "autopilot"
     * MVP : seule la valeur "import" est utilisée.
     */
    @Column(name = "change_source", nullable = false, length = 50)
    @Builder.Default
    private String changeSource = "import";
}
