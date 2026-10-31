package dev.bera.app.repository;

import dev.bera.app.model.Delivery;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select x from Delivery x where x.id=:id")
  Optional<Delivery> lock(@Param("id") UUID id);

  List<Delivery> findTop100ByOrderByCreatedAtDesc();

  @Query(
    value = "SELECT * FROM deliveries WHERE status='QUEUED' AND next_attempt_at<=now() ORDER BY next_attempt_at LIMIT 1 FOR UPDATE SKIP LOCKED",
    nativeQuery = true
  )
  Optional<Delivery> claim();
}
