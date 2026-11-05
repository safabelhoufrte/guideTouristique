package ma.tourisme.avis;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import ma.tourisme.avis.client.ReservationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AvisSecurityIntegrationTest.TestJwt.class)
class AvisSecurityIntegrationTest {

  static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:16")
      .withEnv("GUIDE_DB_PASSWORD", "test-guide-only")
      .withEnv("CIRCUIT_DB_PASSWORD", "test-circuit-only")
      .withEnv("RESERVATION_DB_PASSWORD", "test-reservation-only")
      .withEnv("AVIS_DB_PASSWORD", "test-avis-only")
      .withCopyFileToContainer(
          org.testcontainers.utility.MountableFile.forHostPath(
              java.nio.file.Path.of("../db/init.sql").toAbsolutePath()
          ),
          "/docker-entrypoint-initdb.d/init.sql"
      );

  static {
    DB.start();
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", () -> "jdbc:postgresql://" + DB.getHost() + ":" + DB.getMappedPort(5432) + "/avis_db");
    r.add("spring.datasource.username", () -> "avis");
    r.add("spring.datasource.password", () -> "test-avis-only");
  }

  @LocalServerPort
  int port;

  @Autowired
  JdbcTemplate jdbc;

  final ObjectMapper json = new ObjectMapper();
  static final HttpClient http = HttpClient.newHttpClient();

  @TestConfiguration
  static class TestJwt {
    @Bean
    @Primary
    JwtDecoder decoder() {
      return token -> {
        String role = token.startsWith("admin") ? "ADMIN" : token.startsWith("guide") ? "GUIDE" : "USER";
        String sub = token.equals("user") ? "11111111-1111-4111-8111-111111111111"
            : token.equals("guide") ? "22222222-2222-4222-8222-222222222222"
            : token.equals("admin") ? "33333333-3333-4333-8333-333333333333"
            : "44444444-4444-4444-8444-444444444444";
        return Jwt.withTokenValue(token)
            .header("alg", "test")
            .subject(sub)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .claim("realm_access", Map.of("roles", List.of(role)))
            .claim("name", "Test Tourist")
            .build();
      };
    }
  }

  HttpResponse<String> call(String method, String path, String body, String token) throws Exception {
    var req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
        .header("Content-Type", "application/json");
    if (token != null) {
      req.header("Authorization", "Bearer " + token);
    }
    return http.send(
        req.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(),
        HttpResponse.BodyHandlers.ofString()
    );
  }

  JsonNode expect(String method, String path, String body, String token, int status) throws Exception {
    var response = call(method, path, body, token);
    assertEquals(status, response.statusCode(), "Path: " + path + " - Body: " + response.body());
    return response.body().isBlank() ? json.nullNode() : json.readTree(response.body());
  }

  @MockitoBean
  ReservationClient reservations;

  @BeforeEach
  void clear() {
    jdbc.update("DELETE FROM avis");
    when(reservations.requireEligible(eq(25L), any()))
        .thenReturn(new ReservationClient.References(10L, 7L));
  }

  String request() {
    return "{\"reservationId\":25,\"noteGuide\":5,\"noteCircuit\":4,\"commentaire\":\"Très belle visite\"}";
  }

  @Test
  void testWorkflowCompletEtAjustements() throws Exception {
    // 1. Sans token : 401
    expect("POST", "/api/avis", request(), null, 401);

    // 2. Création par un utilisateur : 201
    var saved = expect("POST", "/api/avis", request(), "user", 201);
    long id = saved.path("id").asLong();
    assertEquals(10, saved.path("circuitId").asInt());
    assertEquals(7, saved.path("guideId").asInt());
    assertEquals("Test Tourist", saved.path("auteur").asText());
    assertFalse(saved.has("userSubject"));

    // 3. Doublon pour la même réservation : 409
    expect("POST", "/api/avis", request(), "user", 409);

    // 4. Note hors limites : 400
    expect("POST", "/api/avis", request().replace("\"noteGuide\":5", "\"noteGuide\":6"), "user", 400);

    // 5. GET /api/avis/{id} pour avis publié : 200 pour tout le monde
    expect("GET", "/api/avis/" + id, null, null, 200);
    expect("GET", "/api/avis/" + id, null, "user", 200);

    // 6. Moyenne pour circuit et guide (avis publié)
    var moyCircuit = expect("GET", "/api/avis/moyenne?circuitId=10", null, null, 200);
    assertEquals(4.0, moyCircuit.path("moyenne").asDouble());
    assertEquals(1, moyCircuit.path("nombreAvis").asLong());

    var moyGuide = expect("GET", "/api/avis/moyenne?guideId=7", null, null, 200);
    assertEquals(5.0, moyGuide.path("moyenne").asDouble());
    assertEquals(1, moyGuide.path("nombreAvis").asLong());

    // 7. Modification publication : interdit aux non-admins
    expect("PATCH", "/api/avis/" + id + "/publication", "{\"publie\":false}", "user", 403);

    // 8. Modification publication par ADMIN : 200
    expect("PATCH", "/api/avis/" + id + "/publication", "{\"publie\":false}", "admin", 200);

    // 9. Ajustement 3 : un avis non publié renvoie 404 aux non-admins et 200 aux admins
    expect("GET", "/api/avis/" + id, null, null, 404);
    expect("GET", "/api/avis/" + id, null, "user", 404);
    expect("GET", "/api/avis/" + id, null, "admin", 200);

    // 10. Liste publique exclut l'avis masqué, admin voit tout
    assertEquals(0, expect("GET", "/api/avis", null, null, 200).size());
    assertEquals(1, expect("GET", "/api/avis/admin", null, "admin", 200).size());

    // 11. Isolation /me
    assertEquals(1, expect("GET", "/api/avis/me", null, "user", 200).size());
    assertEquals(0, expect("GET", "/api/avis/me", null, "other", 200).size());

    // 12. Suppression : interdit aux users, autorisé aux admins
    expect("DELETE", "/api/avis/" + id, null, "user", 403);
    call("DELETE", "/api/avis/" + id, null, "admin");
    expect("GET", "/api/avis/" + id, null, "admin", 404);
  }

  @Test
  void testSimulationErreursReservation() throws Exception {
    // Visite appartenant à un autre touriste
    when(reservations.requireEligible(eq(99L), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette réservation ne vous appartient pas."));
    expect("POST", "/api/avis", request().replace("25", "99"), "user", 403);

    // Visite non éligible (date future ou non acceptée)
    when(reservations.requireEligible(eq(98L), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Un avis est possible après une visite acceptée."));
    expect("POST", "/api/avis", request().replace("25", "98"), "user", 409);

    // Service réservation indisponible
    when(reservations.requireEligible(eq(97L), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service indisponible"));
    expect("POST", "/api/avis", request().replace("25", "97"), "user", 503);

    assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM avis", Integer.class));
  }
}
