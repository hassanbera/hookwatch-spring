package dev.bera.app.controller;

import com.fasterxml.jackson.databind.JsonNode;
import dev.bera.app.service.DomainService;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {

  private final DomainService service;
  private final JdbcTemplate db;

  public ApiController(DomainService service, JdbcTemplate db) {
    this.service = service;
    this.db = db;
  }

  @GetMapping("/health")
  public Object health() {
    db.queryForObject("SELECT 1", Integer.class);
    return Map.of("data", Map.of("status", "ok"));
  }

  @GetMapping("/endpoints")
  public Object action0() {
    return Map.of("data", service.endpoints());
  }

  @PostMapping("/endpoints")
  public Object action1(@RequestBody JsonNode j) {
    return Map.of("data", service.endpoint(j));
  }

  @GetMapping("/deliveries")
  public Object action2() {
    return Map.of("data", service.deliveries());
  }

  @PostMapping("/deliveries")
  public Object action3(@RequestBody JsonNode j) {
    return Map.of("data", service.create(j));
  }

  @GetMapping("/deliveries/{id}")
  public Object action4(@PathVariable UUID id) {
    return Map.of("data", service.delivery(id));
  }

  @PostMapping("/deliveries/{id}/retry")
  public Object action5(@PathVariable UUID id) {
    return Map.of("data", service.retry(id));
  }

  @PostMapping("/demo/receiver")
  public Object receiver(@RequestBody JsonNode j) {
    return Map.of("data", Map.of("received", true));
  }
}
