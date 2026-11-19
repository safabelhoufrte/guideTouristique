package ma.tourisme.avis.client;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tests unitaires de ReservationClient.
 * reservation-service est simule par un mini serveur HTTP local (JDK) :
 * on controle le code HTTP et le corps JSON renvoyes, sans service reel ni base de donnees.
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
    server.createContext("/api/reservations/25/eligibilite", exchange -> {
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
    jwt = Jwt.withTokenValue("token-test").header("alg", "none").subject("user-1").build();
  }

  @AfterEach
  void tearDown() {
    try {
      server.stop(0);
    } catch (RuntimeException ignored) {
      // serveur deja arrete dans le test d'indisponibilite
    }
  }

  // ---------- Methodes utilitaires ----------

  private void reponse(int codeHttp, String corps) {
    this.status = codeHttp;
    this.body = corps;
  }

  private static String json(String eligible, long reservationId, String circuitId, String guideId) {
    return "{\"eligible\":" + eligible
        + ",\"reservationId\":" + reservationId
        + (circuitId == null ? "" : ",\"circuitId\":" + circuitId)
        + (guideId == null ? "" : ",\"guideId\":" + guideId)
        + "}";
  }

  private static void assertStatus(int attendu, Executable action) {
    ResponseStatusException ex = assertThrows(ResponseStatusException.class, action);
    assertEquals(attendu, ex.getStatusCode().value());
  }

  // ---------- 3.5 requireEligible ----------

  @Test
  void testReservationEligibleRetourneLesReferences() {
    reponse(200, json("true", 25, "10", "7"));

    ReservationClient.References refs = client.requireEligible(25L, jwt);

    assertEquals(10L, refs.circuitId());
    assertEquals(7L, refs.guideId());
    assertEquals("Bearer token-test", authorization);
  }

  @Test
  void testReservationNonEligibleRenvoie403() {
    reponse(200, json("false", 25, "10", "7"));

    assertStatus(403, () -> client.requireEligible(25L, jwt));
  }

  @Test
  void testFindUserReservations() {
    server.createContext("/api/reservations", exchange -> {
      byte[] bytes = "[{\"id\":25,\"circuitId\":10,\"guideId\":7,\"statut\":\"ACCEPTEE\",\"dateVisite\":\"2026-11-01\"}]".getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, bytes.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(bytes);
      }
      exchange.close();
    });
    Jwt jwtWithEmail = Jwt.withTokenValue("token-test").header("alg", "none").subject("user-1").claim("email", "test@test.com").build();
    var res = client.findUserReservations(jwtWithEmail);
    assertEquals(1, res.size());
    assertEquals(25L, res.get(0).id());
    assertEquals(10L, res.get(0).circuitId());
  }

  @Test
  void testReservationDUnAutreTouristeRenvoie403() {
    reponse(403, "");

    assertStatus(403, () -> client.requireEligible(25L, jwt));
  }

  @Test
  void testReservationInexistanteRenvoie400() {
    reponse(404, "");

    assertStatus(400, () -> client.requireEligible(25L, jwt));
  }

  @Test
  void testErreurServeurRenvoie503() {
    reponse(500, "");

    assertStatus(503, () -> client.requireEligible(25L, jwt));
  }

  @Test
  void testReservationIdIncoherentRenvoie503() {
    reponse(200, json("true", 26, "10", "7"));

    assertStatus(503, () -> client.requireEligible(25L, jwt));
  }

  @Test
  void testCircuitIdAbsentRenvoie503() {
    reponse(200, json("true", 25, null, "7"));

    assertStatus(503, () -> client.requireEligible(25L, jwt));
  }

  @Test
  void testGuideIdInferieurA1Renvoie503() {
    reponse(200, json("true", 25, "10", "0"));

    assertStatus(503, () -> client.requireEligible(25L, jwt));
  }

  @Test
  void testChampEligibleNonBooleenRenvoie503() {
    reponse(200, json("\"oui\"", 25, "10", "7"));

    assertStatus(503, () -> client.requireEligible(25L, jwt));
  }

  @Test
  void testServiceInjoignableRenvoie503() {
    int port = server.getAddress().getPort();
    server.stop(0);
    ReservationClient horsLigne = new ReservationClient("http://127.0.0.1:" + port);

    assertStatus(503, () -> horsLigne.requireEligible(25L, jwt));
  }
}