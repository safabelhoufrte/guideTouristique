package ma.tourisme.circuit;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/** Real PostgreSQL/Flyway/HTTP, with a Guide simulator matching main2's API contract. */
class CircuitIntegrationTest {
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16")
          .withEnv("GUIDE_DB_PASSWORD", "test-guide-only")
          .withEnv("CIRCUIT_DB_PASSWORD", "test-circuit-only")
          .withEnv("RESERVATION_DB_PASSWORD", "test-reservation-only")
          .withEnv("AVIS_DB_PASSWORD", "test-avis-only")
          .withCopyFileToContainer(
              MountableFile.forHostPath(Path.of("../db/init.sql").toAbsolutePath()),
              "/docker-entrypoint-initdb.d/init.sql");
  static final ObjectMapper JSON = new ObjectMapper();
  static final HttpClient HTTP = HttpClient.newHttpClient();
  static ConfigurableApplicationContext app;
  static HttpServer guide;
  static ExecutorService workers;
  static int port;

  static String databaseUrl(String database) {
    return "jdbc:postgresql://"
        + POSTGRES.getHost()
        + ":"
        + POSTGRES.getMappedPort(5432)
        + "/"
        + database;
  }

  @BeforeAll
  static void start() throws Exception {
    POSTGRES.start();
    guide = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    workers = Executors.newCachedThreadPool();
    guide.setExecutor(workers);
    guide.createContext(
        "/api/guides/",
        exchange -> {
          long id =
              Long.parseLong(exchange.getRequestURI().getPath().substring("/api/guides/".length()));
          int status = 200;
          String body =
              switch ((int) id) {
                case 1 ->
                    "{\"id\":1,\"nom\":\"Benali\",\"prenom\":\"Youssef\",\"ville\":\"Agadir\",\"langues\":[\"français\"],\"email\":\"youssef@example.com\",\"telephone\":\"0600000000\",\"tarifParJour\":400.0,\"disponible\":true}";
                case 2 -> "{\"id\":2,\"disponible\":false}";
                case 3 -> {
                  status = 404;
                  yield "{\"message\":\"Guide introuvable\"}";
                }
                case 4 -> {
                  status = 503;
                  yield "{}";
                }
                case 5 -> "{\"id\":5}";
                case 6 -> "{\"id\":999,\"disponible\":true}";
                case 7 -> "not-json";
                case 8 -> {
                  status = 204;
                  yield "";
                }
                case 9 -> {
                  try {
                    Thread.sleep(3800);
                  } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                  }
                  yield "{\"id\":9,\"disponible\":true}";
                }
                case 10 -> "{\"disponible\":true}";
                case 12 -> "{\"id\":12,\"disponible\":\"true\"}";
                case 13 -> "{\"id\":\"13\",\"disponible\":true}";
                case 14 -> "{\"id\":14,\"disponible\":1}";
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
            /* timeout test disconnects */
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
                "--spring.datasource.url=" + databaseUrl("circuit_db"),
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

  static JsonNode expect(String method, String path, String body, int status) throws Exception {
    var response = request(method, path, body);
    assertEquals(status, response.statusCode(), response.body());
    return response.body().isBlank() ? JSON.nullNode() : JSON.readTree(response.body());
  }

  static String input(long guideId) {
    return "{\"titre\":\"Tour Agadir\",\"description\":\"Visite guidée\",\"ville\":\"Agadir\",\"dureeHeures\":4,\"prix\":300.50,\"guideId\":%d}"
        .formatted(guideId);
  }

  static long create(long guideId) throws Exception {
    return expect("POST", "/api/circuits", input(guideId), 201).get("id").asLong();
  }

  @Test
  void createsAndReadsExactTeamContract() throws Exception {
    long id = create(1);
    JsonNode row = expect("GET", "/api/circuits/" + id, null, 200);
    assertEquals(7, row.size());
    assertTrue(row.has("description"));
    assertEquals("Agadir", row.path("ville").asText());
    assertEquals(4, row.path("dureeHeures").asInt());
    assertEquals(1, row.path("guideId").asLong());
    assertEquals(300.5, row.path("prix").asDouble());
    assertFalse(row.has("guide_id"));
    assertFalse(row.has("disponible"));
  }

  @Test
  void filtersByVilleAndGuideTogether() throws Exception {
    create(1);
    expect("POST", "/api/circuits", input(2).replace("Agadir", "Rabat"), 201);
    JsonNode rows = expect("GET", "/api/circuits?ville=Rabat&guideId=2", null, 200);
    assertFalse(rows.isEmpty());
    for (JsonNode row : rows) {
      assertEquals("Rabat", row.path("ville").asText());
      assertEquals(2, row.path("guideId").asLong());
    }
    assertTrue(expect("GET", "/api/circuits?ville=Rabat&guideId=999999", null, 200).isEmpty());
  }

  @Test
  void updatesAndDeletesWithoutLosingStoredHistory() throws Exception {
    long id = create(1);
    expect("PUT", "/api/circuits/" + id, input(1).replace("Tour Agadir", "Kasbah"), 200);
    assertEquals("Kasbah", expect("GET", "/api/circuits/" + id, null, 200).path("titre").asText());
    expect("DELETE", "/api/circuits/" + id, null, 204);
    expect("GET", "/api/circuits/" + id, null, 404);
    expect("PUT", "/api/circuits/" + id, input(1), 404);
    expect("DELETE", "/api/circuits/" + id, null, 404);
    for (JsonNode row : expect("GET", "/api/circuits", null, 200))
      assertNotEquals(id, row.path("id").asLong());
    try (var conn =
            DriverManager.getConnection(databaseUrl("circuit_db"), "circuit", "test-circuit-only");
        var query = conn.prepareStatement("SELECT disponible FROM circuit.circuits WHERE id=?")) {
      query.setLong(1, id);
      try (var row = query.executeQuery()) {
        assertTrue(row.next());
        assertFalse(row.getBoolean(1));
      }
    }
  }

  @Test
  void rejectsInvalidMissingAndLegacyInput() throws Exception {
    for (String bad :
        new String[] {
          input(1).replace("300.50", "-1"),
          input(1).replace("300.50", "1.234"),
          input(0),
          input(1).replace("guideId", "guide_id"),
          input(1).replace(",\"guideId\":1", ""),
          input(1).replace("Tour Agadir", ""),
          input(1).replace("\"ville\":\"Agadir\"", "\"ville\":null"),
          input(1).replace("\"dureeHeures\":4", "\"dureeHeures\":0"),
          input(1).replace("\"dureeHeures\":4", "\"dureeHeures\":4.5"),
          input(1).replace("}", ",\"admin\":true}")
        }) expect("POST", "/api/circuits", bad, 400);
  }

  @Test
  void missingCircuitAndInvalidPathReturnTeamErrors() throws Exception {
    JsonNode error = expect("GET", "/api/circuits/999999", null, 404);
    assertEquals(5, error.size());
    for (String field : Set.of("timestamp", "status", "error", "message", "path"))
      assertTrue(error.has(field));
    assertEquals("Circuit introuvable", error.path("message").asText());
    assertEquals("/api/circuits/999999", error.path("path").asText());
    expect("GET", "/api/circuits/invalid", null, 400);
    expect("PUT", "/api/circuits/999999", input(1), 404);
  }

  @Test
  void missingGuideRejectsWritesWith400() throws Exception {
    int before = expect("GET", "/api/circuits", null, 200).size();
    expect("POST", "/api/circuits", input(3), 400);
    assertEquals(before, expect("GET", "/api/circuits", null, 200).size());
    long id = create(1);
    expect("PUT", "/api/circuits/" + id, input(3), 400);
    assertEquals(1, expect("GET", "/api/circuits/" + id, null, 200).path("guideId").asLong());
  }

  @Test
  void guideAvailabilityIsNotConfusedWithGuideExistence() throws Exception {
    long id = create(2);
    assertEquals(2, expect("GET", "/api/circuits/" + id, null, 200).path("guideId").asLong());
  }

  @Test
  void malformedAndUnavailableGuideReturn503WithoutInserting() throws Exception {
    int before = expect("GET", "/api/circuits", null, 200).size();
    for (long guideId : new long[] {4, 5, 6, 7, 8, 10, 12, 13, 14, 999})
      expect("POST", "/api/circuits", input(guideId), 503);
    assertEquals(before, expect("GET", "/api/circuits", null, 200).size());
  }

  @Test
  void slowGuideHasBoundedTimeout() throws Exception {
    long start = System.nanoTime();
    expect("POST", "/api/circuits", input(9), 503);
    assertTrue(Duration.ofNanos(System.nanoTime() - start).toSeconds() < 7);
  }

  @Test
  void angularCorsWorksForTheAgreedOrigin() throws Exception {
    var response =
        HTTP.send(
            HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/circuits"))
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());
    assertEquals(
        "http://localhost:4200",
        response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
  }

  @Test
  void servesSwaggerHealthAndCorrectOpenApi() throws Exception {
    assertEquals("UP", expect("GET", "/actuator/health", null, 200).path("status").asText());
    assertEquals(200, request("GET", "/swagger-ui/index.html", null).statusCode());
    JsonNode spec = expect("GET", "/v3/api-docs", null, 200);
    assertTrue(spec.path("paths").has("/api/circuits/{id}"));
    JsonNode properties =
        spec.path("components").path("schemas").path("CircuitRequest").path("properties");
    for (String field : Set.of("titre", "description", "ville", "dureeHeures", "prix", "guideId"))
      assertTrue(properties.has(field));
    assertFalse(properties.has("guide_id"));
    Files.writeString(Path.of("target/circuit.openapi.json"), spec.toString());
  }

  @Test
  void flywayRunsOnceAndRestartKeepsData() throws Exception {
    long id = create(1);
    app.close();
    startApplication();
    assertEquals(id, expect("GET", "/api/circuits/" + id, null, 200).path("id").asLong());
    try (var conn =
            DriverManager.getConnection(databaseUrl("circuit_db"), "circuit", "test-circuit-only");
        var statement = conn.createStatement();
        var row =
            statement.executeQuery(
                "SELECT count(*) FROM circuit.flyway_circuit_history WHERE success AND version IN"
                    + " ('1','2')")) {
      assertTrue(row.next());
      assertEquals(2, row.getInt(1));
    }
  }

  @Test
  void eachServiceOwnsItsDatabaseAndCannotConnectToTheOthers() throws Exception {
    for (String role : new String[] {"guide", "circuit", "reservation", "avis"}) {
      try (var conn =
              DriverManager.getConnection(
                  databaseUrl(role + "_db"), role, "test-" + role + "-only");
          var statement = conn.createStatement()) {
        try (var row =
            statement.executeQuery(
                "SELECT current_user, current_database(), rolsuper, rolcreatedb, rolcreaterole FROM"
                    + " pg_roles WHERE rolname=current_user")) {
          assertTrue(row.next());
          assertEquals(role, row.getString(1));
          assertEquals(role + "_db", row.getString(2));
          assertFalse(row.getBoolean(3));
          assertFalse(row.getBoolean(4));
          assertFalse(row.getBoolean(5));
        }
        conn.setAutoCommit(false);
        statement.execute("CREATE TABLE public.migration_access_probe(id integer PRIMARY KEY)");
        statement.execute("INSERT INTO public.migration_access_probe VALUES(1)");
        conn.rollback();
      }
      for (String other : new String[] {"guide", "circuit", "reservation", "avis"}) {
        if (role.equals(other)) continue;
        SQLException error =
            assertThrows(
                SQLException.class,
                () -> {
                  try (var ignored =
                      DriverManager.getConnection(
                          databaseUrl(other + "_db"), role, "test-" + role + "-only")) {}
                });
        assertEquals("42501", error.getSQLState());
      }
    }
  }

  @Test
  void additiveMigrationPreservesLegacyRows() throws Exception {
    try (var conn =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var statement = conn.createStatement()) {
      statement.execute("CREATE DATABASE legacy_circuit_test");
    }
    String url = databaseUrl("legacy_circuit_test");
    Flyway.configure()
        .dataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword())
        .schemas("circuit")
        .defaultSchema("circuit")
        .table("flyway_circuit_history")
        .locations("classpath:db/migration/circuit")
        .target("1")
        .load()
        .migrate();
    try (var conn =
            DriverManager.getConnection(url, POSTGRES.getUsername(), POSTGRES.getPassword());
        var statement = conn.createStatement()) {
      statement.execute(
          "INSERT INTO circuit.circuits(titre,prix,guide_id,disponible)"
              + " VALUES('Legacy',100,1,TRUE)");
    }
    Flyway.configure()
        .dataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword())
        .schemas("circuit")
        .defaultSchema("circuit")
        .table("flyway_circuit_history")
        .locations("classpath:db/migration/circuit")
        .load()
        .migrate();
    try (var conn =
            DriverManager.getConnection(url, POSTGRES.getUsername(), POSTGRES.getPassword());
        var statement = conn.createStatement();
        var row =
            statement.executeQuery(
                "SELECT titre,description,ville,duree_heures FROM circuit.circuits")) {
      assertTrue(row.next());
      assertEquals("Legacy", row.getString(1));
      assertNull(row.getObject(2));
      assertNull(row.getObject(3));
      assertNull(row.getObject(4));
    }
  }
}
