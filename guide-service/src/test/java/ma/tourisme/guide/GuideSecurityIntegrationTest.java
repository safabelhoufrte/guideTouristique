package ma.tourisme.guide;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.*;
import java.net.URI;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(GuideSecurityIntegrationTest.TestJwt.class)
class GuideSecurityIntegrationTest {

  static final PostgreSQLContainer<?> DB=new PostgreSQLContainer<>("postgres:16")
      .withEnv("GUIDE_DB_PASSWORD", "test-guide-only")
      .withEnv("CIRCUIT_DB_PASSWORD", "test-circuit-only")
      .withEnv("RESERVATION_DB_PASSWORD", "test-reservation-only")
      .withEnv("AVIS_DB_PASSWORD", "test-avis-only")
      .withCopyFileToContainer(org.testcontainers.utility.MountableFile.forHostPath(
          java.nio.file.Path.of("../db/init.sql").toAbsolutePath()),
          "/docker-entrypoint-initdb.d/init.sql");
  static { DB.start(); }
  @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("spring.datasource.url",()->"jdbc:postgresql://"+DB.getHost()+":"+DB.getMappedPort(5432)+"/tourisme_db");r.add("spring.datasource.username",()->"guide");r.add("spring.datasource.password",()->"test-guide-only");}
  @LocalServerPort int port;
  @Autowired JdbcTemplate jdbc;
  final ObjectMapper json=new ObjectMapper();
  static final HttpClient http=HttpClient.newHttpClient();
  @TestConfiguration static class TestJwt {
    @Bean @Primary JwtDecoder decoder(){return token->{
      String role=token.startsWith("admin")?"ADMIN":token.startsWith("guide")?"GUIDE":"USER";
      String sub=token.equals("user")?"11111111-1111-4111-8111-111111111111":token.equals("guide")?"22222222-2222-4222-8222-222222222222":token.equals("admin")?"33333333-3333-4333-8333-333333333333":"44444444-4444-4444-8444-444444444444";
      return Jwt.withTokenValue(token).header("alg","test").subject(sub).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).claim("realm_access",Map.of("roles",List.of(role))).claim("name","Test Tourist").build();
    };}
  }
  HttpResponse<String> call(String method,String path,String body,String token)throws Exception{
    var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).header("Content-Type","application/json");
    if(token!=null)req.header("Authorization","Bearer "+token);
    return http.send(req.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
  }
  JsonNode expect(String method,String path,String body,String token,int status)throws Exception{
    var response=call(method,path,body,token);assertEquals(status,response.statusCode(),response.body());return response.body().isBlank()?json.nullNode():json.readTree(response.body());
  }

  @BeforeEach void clear(){jdbc.update("DELETE FROM guides");}
  String request(String sub){return "{\"nom\":\"Guide test\",\"ville\":\"Agadir\",\"langues\":[\"Français\"],\"tarifParJour\":400,\"disponible\":true,\"ownerSubject\":"+(sub==null?"null":"\""+sub+"\"")+"}";}
  @Test void rolesAndPublicPrivacy()throws Exception{
    expect("POST","/api/guides",request(null),null,401);
    expect("POST","/api/guides",request(null),"user",403);
    long id=expect("POST","/api/guides",request("22222222-2222-4222-8222-222222222222"),"admin",201).path("id").asLong();
    var publicGuide=expect("GET","/api/guides/"+id,null,null,200);
    assertFalse(publicGuide.has("ownerSubject"));assertFalse(publicGuide.has("email"));
    assertEquals(id,expect("GET","/api/guides/me",null,"guide",200).path("id").asLong());
    expect("GET","/api/guides/me",null,"user",403);
    expect("GET","/api/guides/admin",null,"guide",403);
    expect("PUT","/api/guides/"+id,request(null),"guide",403);
  }
  @Test void profileCannotRebindOrSetPrice()throws Exception{
    expect("POST","/api/guides",request("22222222-2222-4222-8222-222222222222"),"admin",201);
    String profile="{\"nom\":\"Nouveau\",\"ville\":\"Rabat\",\"langues\":[\"Arabe\"],\"disponible\":false}";
    var changed=expect("PUT","/api/guides/me",profile,"guide",200);
    assertFalse(changed.path("disponible").asBoolean());assertEquals(400,changed.path("tarifParJour").asInt());
    expect("PUT","/api/guides/me",profile.replace("}",",\"tarifParJour\":0}"),"guide",400);
    expect("POST","/api/guides",request("22222222-2222-4222-8222-222222222222"),"admin",409);
  }

}
