package dev.bera.app.repository;

import dev.bera.app.model.Endpoint;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface EndpointRepository extends JpaRepository<Endpoint, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select x from Endpoint x where x.id=:id")
  Optional<Endpoint> lock(@Param("id") UUID id);
}
