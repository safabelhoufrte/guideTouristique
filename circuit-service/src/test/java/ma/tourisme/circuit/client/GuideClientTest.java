package ma.tourisme.circuit.client;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Tests du contrat HTTP avec un serveur local simulé, sans Spring ni PostgreSQL. */
class GuideClientTest {
  private HttpServer server;
  private GuideClient client;
  private volatile int status;
  private volatile String body;
  private volatile String authorization;

  @BeforeEach
  void initialiser() throws Exception {
    status = 200;
    body = "";
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/api/guides/7", exchange -> {
      authorization = exchange.getRequestHeaders().getFirst("Authorization");
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
      if (bytes.length > 0) exchange.getResponseBody().write(bytes);
      exchange.close();
    });
    server.start();
    client = new GuideClient("http://127.0.0.1:" + server.getAddress().getPort());
  }

  @AfterEach
  void arreter() { server.stop(0); }

  private void assertStatus(int expected) {
    var error = assertThrows(ResponseStatusException.class, () -> client.requireExisting(7));
    assertEquals(expected, error.getStatusCode().value());
  }

  @Test
  void testGuideExistantDisponibleEtChampsSupplementaires() {
    body = "{\"id\":7,\"disponible\":true,\"nom\":\"Guide test\"}";
    assertDoesNotThrow(() -> client.requireExisting(7));
    assertNull(authorization); // Lecture publique, aucun token propagé.
  }

  @Test
  void testGuideIndisponibleExisteToujours() {
    body = "{\"id\":7,\"disponible\":false}";
    assertDoesNotThrow(() -> client.requireExisting(7));
  }

  @Test
  void testGuideAbsent404Devient400() { status = 404; assertStatus(400); }

  @Test
  void testErreurGuide500Devient503() { status = 500; assertStatus(503); }

  @Test
  void testIdentifiantDifferentRenvoie503() {
    body = "{\"id\":8,\"disponible\":true}"; assertStatus(503);
  }

  @Test
  void testIdentifiantAbsentRenvoie503() {
    body = "{\"disponible\":true}"; assertStatus(503);
  }

  @Test
  void testIdentifiantTexteRenvoie503() {
    body = "{\"id\":\"7\",\"disponible\":true}"; assertStatus(503);
  }

  @Test
  void testIdentifiantHorsLongRenvoie503() {
    body = "{\"id\":9223372036854775808,\"disponible\":true}"; assertStatus(503);
  }

  @Test
  void testDisponibiliteAbsenteRenvoie503() { body = "{\"id\":7}"; assertStatus(503); }

  @Test
  void testDisponibiliteTexteRenvoie503() {
    body = "{\"id\":7,\"disponible\":\"true\"}"; assertStatus(503);
  }

  @Test
  void testTableauAuLieuObjetRenvoie503() {
    body = "[{\"id\":7,\"disponible\":true}]"; assertStatus(503);
  }

  @Test
  void testReponseVideRenvoie503() { assertStatus(503); }

  @Test
  void testJsonInvalideRenvoie503() { body = "pas du JSON"; assertStatus(503); }

  @Test
  void testServiceInjoignableRenvoie503() {
    server.stop(0);
    assertStatus(503);
  }
}
