package ma.tourisme.circuit.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Keycloak resource server: public reads, writes for ADMIN and GUIDE only.
 * Tokens must come from the guide-touristique realm and target guide-touristique-api.
 */
@Configuration
public class SecurityConfig {
  private static final String AUDIENCE = "guide-touristique-api";
  private static final Set<String> PROJECT_ROLES = Set.of("USER", "GUIDE", "ADMIN");

  @Bean
  JwtDecoder jwtDecoder(
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
      @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwks) {
    var decoder = NimbusJwtDecoder.withJwkSetUri(jwks).build();
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefaultWithIssuer(issuer),
        new JwtClaimValidator<List<String>>("aud", aud -> aud != null && aud.contains(AUDIENCE)),
        new JwtClaimValidator<String>("sub", sub -> sub != null && !sub.isBlank())));
    return decoder;
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(SecurityConfig::realmAuthorities);
    // CSRF protects cookie-based sessions. This API is stateless and only accepts
    // Bearer tokens sent explicitly by the client, so there is no cookie to abuse.
    return http.cors(Customizer.withDefaults()).csrf(csrf -> csrf.disable())
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(
            "/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
        .permitAll()
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .requestMatchers(
            HttpMethod.GET, "/api/circuits", "/api/circuits/*", "/api/circuits/*/image")
        .permitAll()
        // GUIDE writes are further restricted to the caller's own circuits in CircuitService.
        .requestMatchers("/api/circuits/**").hasAnyRole("ADMIN", "GUIDE")
        .anyRequest().denyAll())
      .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
      .build();
  }

  /** Project roles from realm_access.roles, as ROLE_USER, ROLE_GUIDE or ROLE_ADMIN. */
  static Collection<GrantedAuthority> realmAuthorities(
      Jwt jwt) {
    List<GrantedAuthority> result = new ArrayList<>();
    Map<String, Object> realm = jwt.getClaimAsMap("realm_access");
    if (realm != null && realm.get("roles") instanceof Collection<?> roles) {
      for (Object role : roles) {
        if (role instanceof String name && PROJECT_ROLES.contains(name)) {
          result.add(new SimpleGrantedAuthority("ROLE_" + name));
        }
      }
    }
    return result;
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    var cors = new CorsConfiguration();
    cors.setAllowedOrigins(List.of("http://localhost:4200"));
    cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cors);
    return source;
  }
}
