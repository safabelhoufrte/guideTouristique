package ma.tourisme.guide.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
 * Keycloak resource server, aligned with circuit-service: the token must come from the
 * guide-touristique realm (issuer), target guide-touristique-api (audience) and carry a subject.
 * Roles are read from realm_access.roles.
 */
@Configuration
public class SecurityConfig {

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwks) {
        var decoder = NimbusJwtDecoder.withJwkSetUri(jwks).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer),
            new JwtClaimValidator<List<String>>("aud",
                aud -> aud != null && aud.contains("guide-touristique-api")),
            new JwtClaimValidator<String>("sub", sub -> sub != null && !sub.isBlank())));
        return decoder;
    }

    /** Keeps only the project roles from realm_access.roles, as ROLE_USER / ROLE_GUIDE / ROLE_ADMIN. */
    public static Collection<GrantedAuthority> realmAuthorities(Jwt jwt) {
        List<GrantedAuthority> result = new ArrayList<>();
        Map<String, Object> realm = jwt.getClaimAsMap("realm_access");
        if (realm != null && realm.get("roles") instanceof Collection<?> roles) {
            for (Object role : roles) {
                if (role instanceof String name && Set.of("USER", "GUIDE", "ADMIN").contains(name)) {
                    result.add(new SimpleGrantedAuthority("ROLE_" + name));
                }
            }
        }
        return result;
    }

    static JwtAuthenticationConverter realmRoles() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(SecurityConfig::realmAuthorities);
        return converter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**",
                    "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // /admin and /me before /{id}, otherwise "me" would be read as an id.
                .requestMatchers(HttpMethod.GET, "/api/guides/admin").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/guides/me").hasRole("GUIDE")
                .requestMatchers(HttpMethod.PUT, "/api/guides/me").hasRole("GUIDE")
                .requestMatchers(HttpMethod.GET, "/api/guides", "/api/guides/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/guides").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/guides/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/guides/*").hasRole("ADMIN")
                .requestMatchers("/error").permitAll()
                .anyRequest().denyAll())
                
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(realmRoles())))
            .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origin:http://localhost:4200}") String origin) {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(origin));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
