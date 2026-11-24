package ma.tourisme.circuit.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class SecurityConfigTest {

  private static Jwt token(Map<String, Object> claims) {
    var builder = Jwt.withTokenValue("t").header("alg", "none").subject("user")
        .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
    claims.forEach(builder::claim);
    return builder.build();
  }

  private static List<String> roles(Jwt jwt) {
    return SecurityConfig.realmAuthorities(jwt).stream().map(GrantedAuthority::getAuthority).toList();
  }

  @Test
  void keepsOnlyTheProjectRolesFromRealmAccess() {
    Jwt jwt = token(Map.of("realm_access",
        Map.of("roles", List.of("ADMIN", "offline_access", "GUIDE", "default-roles"))));
    assertEquals(List.of("ROLE_ADMIN", "ROLE_GUIDE"), roles(jwt));
  }

  @Test
  void givesNoRoleWithoutRealmAccessOrWithMalformedRoles() {
    assertEquals(List.of(), roles(token(Map.of("scope", "openid"))));
    assertEquals(List.of(), roles(token(Map.of("realm_access", Map.of("roles", "ADMIN")))));
    assertEquals(List.of(), roles(token(Map.of("realm_access", Map.of("roles", List.of(42))))));
  }
}
