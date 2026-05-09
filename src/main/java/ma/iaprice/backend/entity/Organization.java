package ma.iaprice.backend.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import ma.iaprice.backend.shared.BaseEntity;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "organizations")
public class Organization extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(length = 100)
    private String sector;

    @Column(length = 10)
    @Builder.Default
    private String country = "MA";

    @Column(length = 10)
    @Builder.Default
    private String currency = "MAD";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
