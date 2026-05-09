package ma.iaprice.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.iaprice.backend.shared.BaseEntity;

import java.time.OffsetDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "subscriptions")
public class Subscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    // active | cancelled | expired
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "active";

    @Column(name = "searches_used", nullable = false)
    @Builder.Default
    private int searchesUsed = 0;

    @Column(name = "period_start", nullable = false)
    @Builder.Default
    private OffsetDateTime periodStart = OffsetDateTime.now();

    @Column(name = "period_end")
    private OffsetDateTime periodEnd;
}
