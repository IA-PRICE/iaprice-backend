package ma.iaprice.backend.repository;

import ma.iaprice.backend.entity.MyPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository pour l'historique de prix.
 * MVP : insertion uniquement (via ImportService).
 */
public interface MyPriceHistoryRepository extends JpaRepository<MyPriceHistory, UUID> {
}
