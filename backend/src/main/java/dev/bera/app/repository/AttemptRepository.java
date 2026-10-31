package dev.bera.app.repository;

import dev.bera.app.model.Attempt;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AttemptRepository extends JpaRepository<Attempt, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select x from Attempt x where x.id=:id")
  Optional<Attempt> lock(@Param("id") UUID id);

  List<Attempt> findByDeliveryIdOrderByNumber(UUID id);
}
