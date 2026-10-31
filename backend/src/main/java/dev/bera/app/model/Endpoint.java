package dev.bera.app.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "endpoints")
public class Endpoint {

  @Id
  public UUID id = UUID.randomUUID();

  @Column(nullable = false)
  public Instant createdAt = Instant.now();

  @Column(name = "name", length = 100)
  public String name = "";

  @Column(name = "url", length = 500)
  public String url = "";
}
