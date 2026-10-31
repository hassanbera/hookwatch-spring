package dev.bera.app.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "attempts")
public class Attempt {

  @Id
  public UUID id = UUID.randomUUID();

  @Column(nullable = false)
  public Instant createdAt = Instant.now();

  @Column(name = "delivery_id")
  public UUID deliveryId = null;

  @Column(name = "number")
  public int number = 0;

  @Column(name = "status_code")
  public Integer statusCode = null;

  @Column(name = "duration_ms")
  public int durationMs = 0;

  @Column(name = "error", length = 500)
  public String error = "";
}
