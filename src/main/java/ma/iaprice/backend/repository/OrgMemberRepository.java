// ─── OrgMemberRepository.java ───────────────────────────────
package ma.iaprice.backend.repository;

import ma.iaprice.backend.entity.OrgMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrgMemberRepository extends JpaRepository<OrgMember, UUID> {
    Optional<OrgMember> findByUser_IdAndOrganization_Id(UUID userId, UUID orgId);
}
