package dev.bera.app.service;

import dev.bera.app.model.*;
import dev.bera.app.repository.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeliveryWorker {

  private final DeliveryRepository deliveries;
  private final EndpointRepository endpoints;
  private final AttemptRepository attempts;
  private final DomainService service;
  private final HttpClient client = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(3))
    .followRedirects(HttpClient.Redirect.NEVER)
    .build();

  public DeliveryWorker(
    DeliveryRepository d,
    EndpointRepository e,
    AttemptRepository a,
    DomainService s
  ) {
    deliveries = d;
    endpoints = e;
    attempts = a;
    service = s;
  }

  @Scheduled(fixedDelay = 1000)
  @Transactional
  public void tick() {
    if ("false".equals(System.getenv("WORKER_ENABLED"))) return;
    var pending = deliveries.claim();
    if (pending.isEmpty()) return;
    var d = pending.get();
    var a = new Attempt();
    a.deliveryId = d.id;
    a.number = ++d.attemptCount;
    d.cycleAttempts++;
    long start = System.nanoTime();
    boolean transientFailure = true;
    try {
      var e = endpoints.findById(d.endpointId).orElseThrow();
      service.allow(e.url);
      var timestamp = Long.toString(Instant.now().getEpochSecond());
      var secret = System.getenv().getOrDefault(
        "WEBHOOK_SIGNING_SECRET",
        "local-signing-key-change-before-deploy"
      );
      var mac = Mac.getInstance("HmacSHA256");
      mac.init(
        new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")
      );
      var signature = HexFormat.of().formatHex(
        mac.doFinal(
          (timestamp + "." + d.payload).getBytes(StandardCharsets.UTF_8)
        )
      );
      var request = HttpRequest.newBuilder(URI.create(e.url))
        .timeout(Duration.ofSeconds(5))
        .header("Content-Type", "application/json")
        .header("X-HookWatch-Id", d.id.toString())
        .header("X-HookWatch-Timestamp", timestamp)
        .header("X-HookWatch-Signature", "sha256=" + signature)
        .POST(HttpRequest.BodyPublishers.ofString(d.payload))
        .build();
      int code = client
        .send(request, HttpResponse.BodyHandlers.discarding())
        .statusCode();
      a.statusCode = code;
      if (code >= 200 && code < 300) d.status = "DELIVERED";
      else {
        a.error = "HTTP " + code;
        transientFailure = code == 408 || code == 429 || code >= 500;
      }
    } catch (Exception e) {
      a.error = "Gönderim başarısız: " + e.getClass().getSimpleName();
    }
    a.durationMs = (int) ((System.nanoTime() - start) / 1000000);
    attempts.save(a);
    if (!d.status.equals("DELIVERED")) {
      if (transientFailure && d.cycleAttempts < 4) {
        d.status = "QUEUED";
        d.nextAttemptAt = Instant.now().plusSeconds(1L << d.cycleAttempts);
      } else d.status = "FAILED";
    }
    deliveries.save(d);
  }
}
