package ma.tourisme.avis.client;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tests unitaires de ReservationClient.
 * reservation-service est simulé par un mini serveur HTTP local (JDK) :
 * simule GET /api/reservations?email=...
 */
class ReservationClientTest {

  private HttpServer server;
  private ReservationClient client;
  private Jwt jwt;

  private volatile int status = 200;
  private volatile String body = "";
  private volatile String authorization;

  @BeforeEach
  void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/api/reservations", exchange -> {
      authorization = exchange.getRequestHeaders().getFirst("Authorization");
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      if (bytes.length == 0) {
        exchange.sendResponseHeaders(status, -1);
      } else {
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
          os.write(bytes);
        }
      }
      exchange.close();
    });
    server.start();
    client = new ReservationClient("http://127.0.0.1:" + server.getAddress().getPort());
    jwt = Jwt.withTokenValue("token-test")
        .header("alg", "none")
        .subject("user-1")
        .claim("email", "touriste@demo.example")
        .build();
  }

  @AfterEach
  void tearDown() {
    try {
      server.stop(0);
    } catch (RuntimeException ignored) {
      // serveur déjà arrêté dans le test d'indisponibilité
    }
  }

  // ---------- Méthodes utilitaires ----------

  private void reponse(int codeHttp, String corps) {
    this.status = codeHttp;
    this.body = corps;
  }

  private static String reservationJson(long id, long circuitId, long guideId, String statut, String dateVisite, String email) {
    return "{"
        + "\"id\":" + id
        + ",\"circuitId\":" + circuitId
        + ",\"guideId\":" + guideId
        + ",\"statut\":\"" + statut + "\""
        + (dateVisite == null ? "" : ",\"dateVisite\":\"" + dateVisite + "\"")
        + (email == null ? "" : ",\"email\":\"" + email + "\"")
        + "}";
  }

  private static String listJson(String... reservations) {
    return "[" + String.join(",", reservations) + "]";
  }

  private static void assertStatus(int attendu, Executable action) {
    ResponseStatusException ex = assertThrows(ResponseStatusException.class, action);
    assertEquals(attendu, ex.getStatusCode().value());
  }

  // ---------- requireEligible ----------

  @Test
  void testReservationEligibleRetourneLesReferences() {
    reponse(200, listJson(reservationJson(25, 10, 7, "ACCEPTEE", "2026-11-01", "touriste@demo.example")));

    ReservationClient.References refs = client.requireEligible(25L, jwt);

    assertEquals(10L, refs.circuitId());
    assertEquals(7L, refs.guideId());
    assertEquals("Bearer token-test", authorization);
  }

  @Test
  void testReservationDUnAutreTouristeRenvoie403() {
    reponse(200, listJson(reservationJson(25, 10, 7, "ACCEPTEE", "2026-11-01", "autre@demo.example")));

    assertStatus(403, () -> client.requireEligible(25L, jwt));
  }

  @Test
  void testStatutEnAttenteRenvoie403() {
    reponse(200, listJson(reservationJson(25, 10, 7, "EN_ATTENTE", "2026-11-01", "touriste@demo.example")));

    assertStatus(403, () -> client.requireEligible(25L, jwt));
  }

  @Test
  void testStatutRefuseeRenvoie403() {
    reponse(200, listJson(reservationJson(25, 10, 7, "REFUSEE", "2026-11-01", "touriste@demo.example")));

    assertStatus(403, () -> client.requireEligible(25L, jwt));
  }

  @Test
  void testStatutAnnuleeRenvoie403() {
    reponse(200, listJson(reservationJson(25, 10, 7, "ANNULEE", "2026-11-01", "touriste@demo.example")));

    assertStatus(403, () -> client.requireEligible(25L, jwt));
  }

  @Test
  void testDateVisiteFutureRenvoie403() {
    String dateFuture = LocalDate.now().plusDays(2).toString();
    reponse(200, listJson(reservationJson(25, 10, 7, "ACCEPTEE", dateFuture, "touriste@demo.example")));

    assertStatus(403, () -> client.requireEligible(25L, jwt));
  }

  @Test
  void testDateVisiteAujourdhuiRenvoie403() {
    String dateAujourdhui = LocalDate.now().toString();
    reponse(200, listJson(reservationJson(25, 10, 7, "ACCEPTEE", dateAujourdhui, "touriste@demo.example")));

    assertStatus(403, () -> client.requireEligible(25L, jwt));
  }

  @Test
  void testReservationIdAbsentDeLaListeRenvoie403() {
    reponse(200, listJson(reservationJson(99, 10, 7, "ACCEPTEE", "2026-11-01", "touriste@demo.example")));

    assertStatus(403, () -> client.requireEligible(25L, jwt));
  }

  @Test
  void testListeReservationsVideRenvoie403() {
    reponse(200, "[]");

    assertStatus(403, () -> client.requireEligible(25L, jwt));
  }

  @Test
  void testEmailManquantDansTokenRenvoie403() {
    Jwt jwtSansEmail = Jwt.withTokenValue("token-test")
        .header("alg", "none")
        .subject("user-1")
        .build();

    assertStatus(403, () -> client.requireEligible(25L, jwtSansEmail));
  }

  @Test
  void testEmailVideDansTokenRenvoie403() {
    Jwt jwtEmailVide = Jwt.withTokenValue("token-test")
        .header("alg", "none")
        .subject("user-1")
        .claim("email", "   ")
        .build();

    assertStatus(403, () -> client.requireEligible(25L, jwtEmailVide));
  }

  @Test
  void testErreurServeurRenvoie503() {
    reponse(500, "");

    assertStatus(503, () -> client.requireEligible(25L, jwt));
  }

  @Test
  void testReponseJsonInvalideRenvoie503() {
    reponse(200, "corps invalide");

    assertStatus(503, () -> client.requireEligible(25L, jwt));
  }

  @Test
  void testReponseNonTableauRenvoie503() {
    reponse(200, "{\"id\":25}");

    assertStatus(503, () -> client.requireEligible(25L, jwt));
  }

  @Test
  void testCircuitIdAbsentRenvoie503() {
    reponse(200, "[{\"id\":25,\"guideId\":7,\"statut\":\"ACCEPTEE\",\"dateVisite\":\"2026-11-01\"}]");

    assertStatus(503, () -> client.requireEligible(25L, jwt));
  }

  @Test
  void testGuideIdInferieurA1Renvoie503() {
    reponse(200, "[{\"id\":25,\"circuitId\":10,\"guideId\":0,\"statut\":\"ACCEPTEE\",\"dateVisite\":\"2026-11-01\"}]");

    assertStatus(503, () -> client.requireEligible(25L, jwt));
  }

  @Test
  void testServiceInjoignableRenvoie503() {
    int port = server.getAddress().getPort();
    server.stop(0);
    ReservationClient horsLigne = new ReservationClient("http://127.0.0.1:" + port);

    assertStatus(503, () -> horsLigne.requireEligible(25L, jwt));
  }

  @Test
  void testFindUserReservations() {
    reponse(200, listJson(reservationJson(25, 10, 7, "ACCEPTEE", "2026-11-01", "touriste@demo.example")));

    var res = client.findUserReservations(jwt);

    assertEquals(1, res.size());
    assertEquals(25L, res.get(0).id());
    assertEquals(10L, res.get(0).circuitId());
    assertEquals(7L, res.get(0).guideId());
    assertEquals("ACCEPTEE", res.get(0).statut());
    assertEquals("touriste@demo.example", res.get(0).email());
  }
}