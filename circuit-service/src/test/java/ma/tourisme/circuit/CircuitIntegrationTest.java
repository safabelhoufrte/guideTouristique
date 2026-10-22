package ma.tourisme.circuit;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/** Actual HTTP, PostgreSQL and Flyway. Guide has no shared Java DTO. Requires Docker. */
class CircuitIntegrationTest {
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withEnv("GUIDE_DB_PASSWORD", "test-guide-only")
          .withEnv("CIRCUIT_DB_PASSWORD", "test-circuit-only")
          .withEnv("RESERVATION_DB_PASSWORD", "test-reservation-only")
          .withEnv("AVIS_DB_PASSWORD", "test-avis-only")
          .withCopyFileToContainer(
              MountableFile.forHostPath(Path.of("../db/init.sql").toAbsolutePath()),
              "/docker-entrypoint-initdb.d/01-init.sql");
  static final ObjectMapper JSON = new ObjectMapper();
  static final HttpClient HTTP = HttpClient.newHttpClient();
  static final AtomicBoolean ACTIVE = new AtomicBoolean(true);
  static ConfigurableApplicationContext app;
  static HttpServer guide;
  static ExecutorService workers;
  static int port;

  @Test
  void teamUsersCanMigrateOnlyTheirOwnSchema() throws Exception {
    for (String role : new String[] {"guide", "circuit", "reservation", "avis"}) {
      try (var connection =
              DriverManager.getConnection(POSTGRES.getJdbcUrl(), role, "test-" + role + "-only");
          var statement = connection.createStatement()) {
        try (var row = statement.executeQuery("SELECT current_user, current_schema()")) {
          assertTrue(row.next());
          assertEquals(role, row.getString(1));
          assertEquals(role, row.getString(2));
        }
        try (var row =
            statement.executeQuery(
                "SELECT rolsuper, rolcreatedb, rolcreaterole FROM pg_roles WHERE"
                    + " rolname=current_user")) {
          assertTrue(row.next());
          assertFalse(row.getBoolean(1));
          assertFalse(row.getBoolean(2));
          assertFalse(row.getBoolean(3));
        }
        // A migration can create/read/write its own tables. Roll back the probe.
        connection.setAutoCommit(false);
        statement.execute("CREATE TABLE migration_access_probe (id integer PRIMARY KEY)");
        statement.executeUpdate("INSERT INTO migration_access_probe VALUES (1)");
        try (var row = statement.executeQuery("SELECT id FROM migration_access_probe")) {
          assertTrue(row.next());
          assertEquals(1, row.getInt(1));
        }
        connection.rollback();
        connection.setAutoCommit(true);
        for (String other : new String[] {"guide", "circuit", "reservation", "avis", "public"}) {
          if (role.equals(other)) continue;
          try (var query =
              connection.prepareStatement(
                  "SELECT has_schema_privilege(current_user, ?, 'USAGE'), "
                      + "has_schema_privilege(current_user, ?, 'CREATE')")) {
            query.setString(1, other);
            query.setString(2, other);
            try (var row = query.executeQuery()) {
              assertTrue(row.next());
              assertFalse(row.getBoolean(1), role + " must not read schema " + other);
              assertFalse(row.getBoolean(2), role + " must not write schema " + other);
            }
          }
          SQLException denied =
              assertThrows(
                  SQLException.class,
                  () ->
                      statement.execute(
                          "CREATE TABLE " + other + ".migration_access_probe(id integer)"));
          assertEquals("42501", denied.getSQLState());
        }
      }
    }
  }

  @BeforeAll
  static void start() throws Exception {
    POSTGRES.start();
    guide = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    workers = Executors.newCachedThreadPool();
    guide.setExecutor(workers);
    guide.createContext(
        "/guides/",
        exchange -> {
          int id = Integer.parseInt(exchange.getRequestURI().getPath().substring(8));
          int status = 200;
          String body =
              switch (id) {
                case 1 ->
                    "{\"id\":1,\"actif\":true,\"nom\":\"Youssef\",\"langues\":[\"fr\"],\"extra\":\"any-language\"}";
                case 2 -> "{\"id\":2,\"actif\":false}";
                case 3 -> {
                  status = 404;
                  yield "{\"detail\":\"absent\"}";
                }
                case 4 -> {
                  status = 503;
                  yield "{}";
                }
                case 5 -> "{\"id\":5}";
                case 6 -> "{\"id\":999,\"actif\":true}";
                case 7 -> "not-json";
                case 8 -> {
                  status = 204;
                  yield "";
                }
                case 9 -> {
                  try {
                    Thread.sleep(3800);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                  yield "{\"id\":9,\"actif\":true}";
                }
                case 10 -> "{\"actif\":true}";
                case 11 -> "{\"id\":11,\"actif\":" + ACTIVE.get() + "}";
                case 12 -> "{\"id\":12,\"actif\":\"true\"}";
                case 13 -> "{\"id\":\"13\",\"actif\":true}";
                case 14 -> "{\"id\":14,\"actif\":1}";
                default -> {
                  status = 500;
                  yield "{}";
                }
              };
          byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
          exchange.sendResponseHeaders(status, status == 204 ? -1 : bytes.length);
          try {
            if (status != 204) exchange.getResponseBody().write(bytes);
          } catch (java.io.IOException ignored) {
            /* timeout test closes the connection */
          } finally {
            exchange.close();
          }
        });
    guide.start();
    startApplication();
  }

  static void startApplication() {
    app =
        new SpringApplicationBuilder(CircuitApplication.class)
            .run(
                "--server.port=0",
                "--server.address=127.0.0.1",
                "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                "--spring.datasource.username=circuit",
                "--spring.datasource.password=test-circuit-only",
                "--services.guide-url=http://127.0.0.1:" + guide.getAddress().getPort(),
                "--spring.main.banner-mode=off",
                "--logging.level.root=WARN");
    port = ((ServletWebServerApplicationContext) app).getWebServer().getPort();
  }

  @AfterAll
  static void stop() {
    if (app != null) app.close();
    if (guide != null) guide.stop(0);
    if (workers != null) workers.shutdownNow();
    POSTGRES.stop();
  }

  static HttpResponse<String> request(String method, String path, String body) throws Exception {
    return HTTP.send(
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .timeout(Duration.ofSeconds(12))
            .header("Content-Type", "application/json")
            .method(
                method,
                body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  static JsonNode expect(String method, String path, String body, int expected) throws Exception {
    var response = request(method, path, body);
    assertEquals(expected, response.statusCode(), response.body());
    return response.body().isBlank() ? JSON.nullNode() : JSON.readTree(response.body());
  }

  static String input(long guideId, boolean available) {
    return "{\"titre\":\"Tour Agadir\",\"prix\":300.50,\"guide_id\":%d,\"disponible\":%s}"
        .formatted(guideId, available);
  }

  static long create(long guideId, boolean available) throws Exception {
    return expect("POST", "/circuits", input(guideId, available), 201).get("id").asLong();
  }

  @Test
  void createsAndReadsCircuitWithoutSharingGuideJavaCode() throws Exception {
    long id = create(1, true);
    JsonNode row = expect("GET", "/circuits/" + id, null, 200);
    assertEquals(300.5, row.get("prix").asDouble());
    assertEquals(1, row.get("guide_id").asLong());
    assertFalse(row.has("guideId"));
    assertTrue(row.get("disponible").asBoolean());
  }

  @Test
  void updatesFiltersAndArchivesWithoutDeletingHistory() throws Exception {
    long id = create(1, true);
    expect("PUT", "/circuits/" + id, input(1, false).replace("Tour Agadir", "Tour Rabat"), 200);
    assertEquals("Tour Rabat", expect("GET", "/circuits/" + id, null, 200).get("titre").asText());
    assertTrue(expect("GET", "/guides/1/circuits", null, 200).size() > 0);
    assertEquals(0, expect("GET", "/circuits?guide_id=999999", null, 200).size());
    expect("DELETE", "/circuits/" + id, null, 204);
    assertFalse(expect("GET", "/circuits/" + id, null, 200).get("disponible").asBoolean());
    expect("GET", "/circuits/" + id + "/reservation-details", null, 409);
    expect("PUT", "/circuits/" + id, input(1, true), 200);
    expect("GET", "/circuits/" + id + "/reservation-details", null, 200);
  }

  @Test
  void rejectsInvalidAndUnknownInputFields() throws Exception {
    expect("POST", "/circuits", input(1, true).replace("300.50", "-1"), 400);
    expect("POST", "/circuits", input(1, true).replace("300.50", "1.234"), 400);
    expect("POST", "/circuits", input(0, true), 400);
    expect("POST", "/circuits", input(1, true).replace("guide_id", "guideId"), 400);
    expect("POST", "/circuits", input(1, true).replace("\"guide_id\":1,", ""), 400);
    expect("POST", "/circuits", input(1, true).replace("\"guide_id\":1", "\"guide_id\":null"), 400);
    expect("POST", "/circuits", input(1, true).replace("Tour Agadir", ""), 400);
    expect("POST", "/circuits", input(1, true).replace("true}", "null}"), 400);
    expect("POST", "/circuits", input(1, true).replace("true}", "true,\"admin\":true}"), 400);
  }

  @Test
  void missingCircuitReturns404() throws Exception {
    expect("GET", "/circuits/999999", null, 404);
    expect("PUT", "/circuits/999999", input(1, true), 404);
    expect("DELETE", "/circuits/999999", null, 404);
    expect("GET", "/circuits/999999/reservation-details", null, 404);
  }

  @Test
  void missingOrInactiveGuideCannotCreateOrUpdate() throws Exception {
    int before = expect("GET", "/circuits", null, 200).size();
    expect("POST", "/circuits", input(2, true), 422);
    expect("POST", "/circuits", input(3, true), 422);
    assertEquals(before, expect("GET", "/circuits", null, 200).size());
    long id = create(1, true);
    expect("PUT", "/circuits/" + id, input(2, false), 422);
    assertTrue(expect("GET", "/circuits/" + id, null, 200).get("disponible").asBoolean());
  }

  @Test
  void invalidGuideResponsesFailClosedWithoutInserting() throws Exception {
    int before = expect("GET", "/circuits", null, 200).size();
    for (long guideId : new long[] {4, 5, 6, 7, 8, 10, 12, 13, 14, 999}) {
      expect("POST", "/circuits", input(guideId, true), 503);
    }
    assertEquals(before, expect("GET", "/circuits", null, 200).size());
  }

  @Test
  void slowGuideIsBoundedByReadTimeout() throws Exception {
    long start = System.nanoTime();
    expect("POST", "/circuits", input(9, true), 503);
    assertTrue(Duration.ofNanos(System.nanoTime() - start).toSeconds() < 7);
  }

  @Test
  void bookingRechecksGuideInsteadOfTrustingOldCreation() throws Exception {
    ACTIVE.set(true);
    long id = create(11, true);
    expect("GET", "/circuits/" + id + "/reservation-details", null, 200);
    ACTIVE.set(false);
    try {
      expect("GET", "/circuits/" + id, null, 200);
      expect("GET", "/circuits/" + id + "/reservation-details", null, 422);
    } finally {
      ACTIVE.set(true);
    }
  }

  @Test
  void unavailableCircuitCanBeCreatedButCannotBeBooked() throws Exception {
    long id = create(1, false);
    expect("GET", "/circuits/" + id + "/reservation-details", null, 409);
  }

  @Test
  void servesHealthSwaggerAndExportableOpenApi() throws Exception {
    assertEquals("UP", expect("GET", "/actuator/health", null, 200).get("status").asText());
    assertEquals(200, request("GET", "/swagger-ui/index.html", null).statusCode());
    var response = request("GET", "/v3/api-docs", null);
    assertEquals(200, response.statusCode());
    assertTrue(
        JSON.readTree(response.body()).get("paths").has("/circuits/{id}/reservation-details"));
    JsonNode schemas = JSON.readTree(response.body()).path("components").path("schemas");
    assertTrue(schemas.path("CircuitRequest").path("properties").has("guide_id"));
    assertTrue(schemas.path("CircuitResponse").path("properties").has("guide_id"));
    Files.writeString(Path.of("target/circuit.openapi.json"), response.body());
  }

  @Test
  void flywayRunsOnceAndRestartPreservesData() throws Exception {
    long id = create(1, true);
    app.close();
    startApplication();
    assertEquals(id, expect("GET", "/circuits/" + id, null, 200).get("id").asLong());
    try (var connection =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var statement = connection.createStatement();
        var result =
            statement.executeQuery(
                "SELECT count(*) FROM circuit.flyway_circuit_history WHERE success AND"
                    + " version='1'")) {
      assertTrue(result.next());
      assertEquals(1, result.getInt(1));
    }
  }
}
