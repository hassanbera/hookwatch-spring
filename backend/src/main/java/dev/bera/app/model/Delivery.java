package dev.bera.app.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "deliveries")
public class Delivery {

  @Id
  public UUID id = UUID.randomUUID();

  @Column(nullable = false)
  public Instant createdAt = Instant.now();

  @Column(name = "endpoint_id")
  public UUID endpointId = null;

  @Column(name = "event_type", length = 80)
  public String eventType = "";

  @Column(name = "payload", columnDefinition = "text")
  public String payload = "{}";

  @Column(name = "status", length = 20)
  public String status = "QUEUED";

  @Column(name = "attempt_count")
  public int attemptCount = 0;

  @Column(name = "cycle_attempts")
  public int cycleAttempts = 0;

  @Column(name = "next_attempt_at")
  public Instant nextAttemptAt = Instant.now();
}
