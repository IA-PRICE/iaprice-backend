package ma.iaprice.backend.repository;

import ma.iaprice.backend.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findByOrganization_IdAndStatus(UUID orgId, String status);
}
