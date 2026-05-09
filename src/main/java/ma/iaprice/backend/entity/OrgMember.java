package ma.iaprice.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.iaprice.backend.shared.BaseEntity;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "org_members",
       uniqueConstraints = @UniqueConstraint(columnNames = {"org_id", "user_id"}))
public class OrgMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Rôles possibles : owner, admin, viewer
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String role = "owner";
}
