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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import ma.tourisme.avis.client.ReservationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
    r.add("spring.datasource.url", () -> "jdbc:postgresql://" + DB.getHost() + ":" + DB.getMappedPort(5432) + "/tourisme_db");
    r.add("spring.datasource.username", () -> "avis");
    r.add("spring.datasource.password", () -> "test-avis-only");
    r.add("spring.jackson.deserialization.fail-on-unknown-properties", () -> "false");
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
        String role;
        if (token.startsWith("admin")) {
          role = "ADMIN";
        } else if (token.startsWith("guide")) {
          role = "GUIDE";
        } else if (token.equals("norole")) {
          role = "OTHER";
        } else {
          role = "USER";
        }

        String sub = switch (token) {
          case "user" -> "11111111-1111-4111-8111-111111111111";
          case "other" -> "22222222-2222-4222-8222-222222222222";
          case "third" -> "33333333-3333-4333-8333-333333333333";
          case "guide" -> "44444444-4444-4444-8444-444444444444";
          case "admin" -> "55555555-5555-4555-8555-555555555555";
          default -> "99999999-9999-4999-8999-999999999999";
        };

        String name = token.equals("user") ? "Test Tourist"
            : token.equals("other") ? "Other Tourist"
            : token.equals("admin") ? "Admin User"
            : "Guide User";

        List<String> roles = token.equals("norole") ? List.of() : List.of(role);

        return Jwt.withTokenValue(token)
            .header("alg", "test")
            .subject(sub)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .claim("realm_access", Map.of("roles", roles))
            .claim("name", name)
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
    when(reservations.requireEligible(eq(26L), any()))
        .thenReturn(new ReservationClient.References(12L, 8L));
  }

  String request() {
    return "{\"reservationId\":25,\"noteGuide\":5,\"noteCircuit\":4,\"commentaire\":\"Très belle visite\"}";
  }

  // 1. Sans token : 401
  @Test
  @DisplayName("1. Sans token : 401 sur les endpoints protégés")
  void testSansTokenRenvoie401() throws Exception {
    expect("POST", "/api/avis", request(), null, 401);
    expect("GET", "/api/avis/me", null, null, 401);
    expect("GET", "/api/avis/admin", null, null, 401);
    expect("PATCH", "/api/avis/1/publication", "{\"publie\":false}", null, 401);
    expect("DELETE", "/api/avis/1", null, null, 401);
  }

  // 2. Mauvais rôle : 403
  @Test
  @DisplayName("2. Mauvais rôle : 403 (GUIDE pour créer/me, USER pour admin/publication/suppression)")
  void testMauvaisRoleRenvoie403() throws Exception {
    // Rôle GUIDE n'a pas accès à POST /api/avis (réservé à USER)
    expect("POST", "/api/avis", request(), "guide", 403);

    // Rôle GUIDE n'a pas accès à GET /api/avis/me (réservé à USER)
    expect("GET", "/api/avis/me", null, "guide", 403);

    // Jeton sans rôle : 403
    expect("POST", "/api/avis", request(), "norole", 403);

    // Rôle USER n'a pas accès aux actions ADMIN
    expect("GET", "/api/avis/admin", null, "user", 403);
    expect("PATCH", "/api/avis/1/publication", "{\"publie\":false}", "user", 403);
    expect("DELETE", "/api/avis/1", null, "user", 403);
  }

  // 3. Visite d'un autre touriste refusée (403)
  @Test
  @DisplayName("3. Visite d'un autre touriste refusée (403 et aucune insertion en base)")
  void testVisiteAutreTouristeRefusee() throws Exception {
    when(reservations.requireEligible(eq(99L), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette réservation ne vous appartient pas."));

    expect("POST", "/api/avis", request().replace("25", "99"), "user", 403);

    assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM avis", Integer.class));
  }

  // 4. Visite future, non acceptée ou inexistante refusée
  @Test
  @DisplayName("4. Visite future, non acceptée (409) ou inexistante (400) refusée")
  void testVisiteFutureNonAccepteeOuInexistanteRefusee() throws Exception {
    // Visite future ou non acceptée -> 409 Conflict
    when(reservations.requireEligible(eq(88L), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Un avis est possible uniquement après la date d’une visite acceptée."));
    expect("POST", "/api/avis", request().replace("25", "88"), "user", 409);

    // Réservation inexistante dans reservation-service -> 400 Bad Request
    when(reservations.requireEligible(eq(87L), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Réservation introuvable."));
    expect("POST", "/api/avis", request().replace("25", "87"), "user", 400);

    assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM avis", Integer.class));
  }

  // 5. Réponse non conforme ou panne de reservation-service -> jamais de publication (503)
  @Test
  @DisplayName("5. Réponse non conforme ou panne de reservation-service -> 503 et jamais de publication")
  void testPanneOuReponseNonConformeReservationServiceRenvoie503SansPublication() throws Exception {
    // Panne réseau ou timeout de reservation-service
    when(reservations.requireEligible(eq(77L), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Le service Réservation est momentanément indisponible."));
    expect("POST", "/api/avis", request().replace("25", "77"), "user", 503);

    // Réponse non conforme (identifiant incorrect, circuit/guide manquant) détectée par le client
    when(reservations.requireEligible(eq(76L), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Le service Réservation est momentanément indisponible."));
    expect("POST", "/api/avis", request().replace("25", "76"), "user", 503);

    assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM avis", Integer.class));
  }

  // 6. Notes hors 1-5 et commentaire vide ou > 2000 caractères rejetés
  @Test
  @DisplayName("6. Notes hors 1-5 et commentaire vide ou > 2000 caractères rejetés (400)")
  void testValidationNotesEtCommentaireInvalidesRejetees400() throws Exception {
    // Note guide < 1
    expect("POST", "/api/avis", request().replace("\"noteGuide\":5", "\"noteGuide\":0"), "user", 400);

    // Note guide > 5
    expect("POST", "/api/avis", request().replace("\"noteGuide\":5", "\"noteGuide\":6"), "user", 400);

    // Note circuit < 1
    expect("POST", "/api/avis", request().replace("\"noteCircuit\":4", "\"noteCircuit\":0"), "user", 400);

    // Note circuit > 5
    expect("POST", "/api/avis", request().replace("\"noteCircuit\":4", "\"noteCircuit\":6"), "user", 400);

    // Commentaire vide
    expect("POST", "/api/avis", request().replace("\"Très belle visite\"", "\"\""), "user", 400);

    // Commentaire espaces blancs
    expect("POST", "/api/avis", request().replace("\"Très belle visite\"", "\"   \""), "user", 400);

    // Commentaire > 2000 caractères
    String longCommentaire = "a".repeat(2001);
    expect("POST", "/api/avis", request().replace("\"Très belle visite\"", "\"" + longCommentaire + "\""), "user", 400);

    // reservationId négatif ou zéro
    expect("POST", "/api/avis", request().replace("\"reservationId\":25", "\"reservationId\":0"), "user", 400);
    expect("POST", "/api/avis", request().replace("\"reservationId\":25", "\"reservationId\":-1"), "user", 400);

    assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM avis", Integer.class));
  }

  // 7. Deux créations simultanées sur la même réservation -> une seule réussit, l'autre 409
  @Test
  @DisplayName("7. Deux créations simultanées sur la même réservation -> une seule réussit (201), l'autre échoue (409)")
  void testDeuxCreationsSimultaneesMemeReservationUneSeuleReussitAutre409() throws Exception {
    int threads = 2;
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    CyclicBarrier barrier = new CyclicBarrier(threads);
    List<Future<HttpResponse<String>>> futures = new ArrayList<>();

    for (int i = 0; i < threads; i++) {
      futures.add(executor.submit(() -> {
        barrier.await();
        return call("POST", "/api/avis", request(), "user");
      }));
    }

    List<Integer> statuses = new ArrayList<>();
    for (Future<HttpResponse<String>> f : futures) {
      statuses.add(f.get(10, TimeUnit.SECONDS).statusCode());
    }
    executor.shutdown();

    assertTrue(statuses.contains(201), "Une des deux créations simultanées doit réussir (201)");
    assertTrue(statuses.contains(409), "L'autre création simultanée doit échouer en conflit (409)");
    assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM avis WHERE reservation_id = 25", Integer.class));
  }

  // 8. /me n'affiche que les avis du compte connecté
  @Test
  @DisplayName("8. /me n'affiche que les avis du compte connecté (isolation touristes)")
  void testMeAfficheUniquementAvisDuCompteConnecte() throws Exception {
    // User 1 crée un avis sur reservation 25
    expect("POST", "/api/avis", request(), "user", 201);

    // User 2 crée un avis sur reservation 26
    String reqUser2 = "{\"reservationId\":26,\"noteGuide\":4,\"noteCircuit\":5,\"commentaire\":\"Magnifique !\"}";
    expect("POST", "/api/avis", reqUser2, "other", 201);

    // User 1 ne voit que le sien
    var avisUser1 = expect("GET", "/api/avis/me", null, "user", 200);
    assertEquals(1, avisUser1.size());
    assertEquals(25, avisUser1.get(0).path("reservationId").asLong());
    assertEquals("Test Tourist", avisUser1.get(0).path("auteur").asText());

    // User 2 ne voit que le sien
    var avisUser2 = expect("GET", "/api/avis/me", null, "other", 200);
    assertEquals(1, avisUser2.size());
    assertEquals(26, avisUser2.get(0).path("reservationId").asLong());
    assertEquals("Other Tourist", avisUser2.get(0).path("auteur").asText());

    // Un troisième utilisateur ne voit rien
    var avisUser3 = expect("GET", "/api/avis/me", null, "third", 200);
    assertEquals(0, avisUser3.size());
  }

  // 9. ADMIN publie/masque et les avis masqués disparaissent de la liste publique
  @Test
  @DisplayName("9. ADMIN publie/masque et les avis masqués disparaissent de la liste publique (et 404 pour non-admin)")
  void testAdminPublieMasqueEtAvisMasquesDisparaissentListePublique() throws Exception {
    var created = expect("POST", "/api/avis", request(), "user", 201);
    long id = created.path("id").asLong();
    assertTrue(created.path("publie").asBoolean());

    // Visible dans la liste publique
    assertEquals(1, expect("GET", "/api/avis", null, null, 200).size());
    expect("GET", "/api/avis/" + id, null, null, 200);

    // ADMIN masque l'avis
    var patched = expect("PATCH", "/api/avis/" + id + "/publication", "{\"publie\":false}", "admin", 200);
    assertFalse(patched.path("publie").asBoolean());

    // L'avis masqué disparaît de la liste publique
    assertEquals(0, expect("GET", "/api/avis", null, null, 200).size());

    // GET /api/avis/{id} renvoie 404 pour un non-admin (visiteur ou USER)
    expect("GET", "/api/avis/" + id, null, null, 404);
    expect("GET", "/api/avis/" + id, null, "user", 404);

    // GET /api/avis/{id} renvoie 200 pour ADMIN
    expect("GET", "/api/avis/" + id, null, "admin", 200);

    // GET /api/avis/admin renvoie tous les avis (y compris masqués)
    assertEquals(1, expect("GET", "/api/avis/admin", null, "admin", 200).size());

    // ADMIN republie l'avis
    expect("PATCH", "/api/avis/" + id + "/publication", "{\"publie\":true}", "admin", 200);
    assertEquals(1, expect("GET", "/api/avis", null, null, 200).size());
    expect("GET", "/api/avis/" + id, null, null, 200);
  }

  // 10. Champs auteur, guideId ou circuitId envoyés dans le corps sont ignorés
  @Test
  @DisplayName("10. Champs auteur, guideId, circuitId, userSubject injectés dans le corps sont ignorés")
  void testChampsAuteurGuideIdCircuitIdEnvoyesSontIgnores() throws Exception {
    String maliciousPayload = "{"
        + "\"reservationId\":25,"
        + "\"noteGuide\":5,"
        + "\"noteCircuit\":4,"
        + "\"commentaire\":\"Très belle visite\","
        + "\"auteur\":\"Hacker_X\","
        + "\"guideId\":9999,"
        + "\"circuitId\":8888,"
        + "\"userSubject\":\"fake-subject-uuid\","
        + "\"publie\":false"
        + "}";

    var saved = expect("POST", "/api/avis", maliciousPayload, "user", 201);
    long id = saved.path("id").asLong();

    // Valeurs issues de reservation-service et du JWT, PAS du corps frelaté
    assertEquals(10, saved.path("circuitId").asLong(), "Le circuitId doit provenir de reservation-service (10)");
    assertEquals(7, saved.path("guideId").asLong(), "Le guideId doit provenir de reservation-service (7)");
    assertEquals("Test Tourist", saved.path("auteur").asText(), "L'auteur doit provenir du JWT (claim name)");
    assertTrue(saved.path("publie").asBoolean(), "Un nouvel avis est publié par défaut");
    assertFalse(saved.has("userSubject"), "userSubject ne doit jamais être exposé dans la réponse");

    // Vérification en base SQL
    Map<String, Object> row = jdbc.queryForMap("SELECT * FROM avis WHERE id = ?", id);
    assertEquals(10L, ((Number) row.get("circuit_id")).longValue());
    assertEquals(7L, ((Number) row.get("guide_id")).longValue());
    assertEquals("Test Tourist", row.get("auteur"));
    assertEquals("11111111-1111-4111-8111-111111111111", row.get("user_subject"));
  }

  // 11. Calcul moyenne avis publiés uniquement
  @Test
  @DisplayName("11. GET /api/avis/moyenne calcule la note moyenne des avis publiés uniquement")
  void testCalculMoyenneAvisPubliesUniquement() throws Exception {
    var saved = expect("POST", "/api/avis", request(), "user", 201);
    long id = saved.path("id").asLong();

    // Moyenne calculée sur avis publié
    var moyCircuit = expect("GET", "/api/avis/moyenne?circuitId=10", null, null, 200);
    assertEquals(4.0, moyCircuit.path("moyenne").asDouble());
    assertEquals(1, moyCircuit.path("nombreAvis").asLong());

    var moyGuide = expect("GET", "/api/avis/moyenne?guideId=7", null, null, 200);
    assertEquals(5.0, moyGuide.path("moyenne").asDouble());
    assertEquals(1, moyGuide.path("nombreAvis").asLong());

    // Masquage de l'avis
    expect("PATCH", "/api/avis/" + id + "/publication", "{\"publie\":false}", "admin", 200);

    // L'avis masqué n'est plus pris en compte dans la moyenne
    var moyCircuitApres = expect("GET", "/api/avis/moyenne?circuitId=10", null, null, 200);
    assertEquals(0.0, moyCircuitApres.path("moyenne").asDouble());
    assertEquals(0, moyCircuitApres.path("nombreAvis").asLong());

    // Requête sans paramètre : 400
    expect("GET", "/api/avis/moyenne", null, null, 400);
  }

  // 12. Suppression réservée à ADMIN
  @Test
  @DisplayName("12. DELETE /api/avis/{id} réservé à ADMIN (403 pour USER, 204 pour ADMIN)")
  void testSuppressionAdminUniquement() throws Exception {
    var saved = expect("POST", "/api/avis", request(), "user", 201);
    long id = saved.path("id").asLong();

    // USER ne peut pas supprimer
    expect("DELETE", "/api/avis/" + id, null, "user", 403);

    // ADMIN peut supprimer
    call("DELETE", "/api/avis/" + id, null, "admin");
    expect("GET", "/api/avis/" + id, null, "admin", 404);
  }
}
