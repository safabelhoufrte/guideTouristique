package ma.tourisme.circuit.config;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {
  @Bean
  JwtDecoder jwtDecoder(
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
      @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwks) {
    var decoder = NimbusJwtDecoder.withJwkSetUri(jwks).build();
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefaultWithIssuer(issuer),
        new JwtClaimValidator<List<String>>("aud", aud -> aud != null && aud.contains("guide-touristique-api")),
        new JwtClaimValidator<String>("sub", sub -> sub != null && !sub.isBlank())));
    return decoder;
  }
  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> {
      List<GrantedAuthority> result = new ArrayList<>();
      Map<String, Object> realm = jwt.getClaimAsMap("realm_access");
      if (realm != null && realm.get("roles") instanceof Collection<?> roles)
        for (Object role : roles)
          if (role instanceof String name && Set.of("USER", "GUIDE", "ADMIN").contains(name))
            result.add(new SimpleGrantedAuthority("ROLE_" + name));
      return result;
    });
    return http.cors(cors -> {}).csrf(csrf -> csrf.disable())
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/circuits", "/api/circuits/*").permitAll()
        .requestMatchers("/api/circuits/**").hasRole("ADMIN")
        .anyRequest().denyAll())
      .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
      .build();
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
