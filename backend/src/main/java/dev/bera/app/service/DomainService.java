package dev.bera.app.service;

import static dev.bera.app.common.Input.*;

import com.fasterxml.jackson.databind.*;
import dev.bera.app.common.*;
import dev.bera.app.model.*;
import dev.bera.app.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DomainService {

  private final EndpointRepository endpoints;
  private final DeliveryRepository deliveries;
  private final AttemptRepository attempts;

  public DomainService(
    EndpointRepository endpoints,
    DeliveryRepository deliveries,
    AttemptRepository attempts
  ) {
    this.endpoints = endpoints;
    this.deliveries = deliveries;
    this.attempts = attempts;
  }

  private final String allowed = System.getenv().getOrDefault(
    "WEBHOOK_ALLOWED_ORIGINS",
    "http://127.0.0.1:" + System.getenv().getOrDefault("PORT", "4205")
  );

  private String origin(String value) {
    try {
      var u = java.net.URI.create(value);
      if (
        u.getHost() == null ||
        u.getUserInfo() != null ||
        u.getFragment() != null ||
        !List.of("http", "https").contains(u.getScheme())
      ) throw new Exception();
      return (
        u.getScheme() +
        "://" +
        u.getHost() +
        (u.getPort() == -1 ? "" : ":" + u.getPort())
      );
    } catch (Exception e) {
      throw new ApiException(400, "Geçersiz hedef URL.");
    }
  }

  public void allow(String url) {
    var o = origin(url);
    if (
      Arrays.stream(allowed.split(",")).map(String::trim).noneMatch(o::equals)
    ) throw new ApiException(400, "Hedef origin izin listesinde değil.");
  }

  public List<Endpoint> endpoints() {
    return endpoints.findAll(
      org.springframework.data.domain.Sort.by("createdAt")
    );
  }

  @Transactional
  public Endpoint endpoint(JsonNode j) {
    var e = new Endpoint();
    e.name = text(j, "name", 100, true);
    e.url = text(j, "url", 500, true);
    allow(e.url);
    return endpoints.save(e);
  }

  public List<Delivery> deliveries() {
    return deliveries.findTop100ByOrderByCreatedAtDesc();
  }

  public Map<String, Object> delivery(UUID id) {
    return Map.of(
      "delivery",
      found(deliveries.findById(id)),
      "attempts",
      attempts.findByDeliveryIdOrderByNumber(id)
    );
  }

  @Transactional
  public Delivery create(JsonNode j) {
    var d = new Delivery();
    d.endpointId = id(j, "endpointId");
    found(endpoints.findById(d.endpointId));
    d.eventType = text(j, "eventType", 80, true);
    var p = j.get("payload");
    if (
      p == null || !p.isObject() || p.toString().length() > 32000
    ) throw new ApiException(400, "JSON nesnesi en fazla 32 KB olmalı.");
    d.payload = p.toString();
    return deliveries.save(d);
  }

  @Transactional
  public Delivery retry(UUID id) {
    var d = found(deliveries.lock(id));
    if (!d.status.equals("FAILED")) throw new ApiException(
      409,
      "Yalnızca başarısız teslimat yeniden denenebilir."
    );
    d.status = "QUEUED";
    d.cycleAttempts = 0;
    d.nextAttemptAt = Instant.now();
    return deliveries.save(d);
  }
}
