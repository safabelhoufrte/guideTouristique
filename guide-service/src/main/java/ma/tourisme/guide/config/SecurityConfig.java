
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
 * Configuration de la sécurité du microservice Guide.
 *
 * <p>Cette configuration utilise Keycloak pour vérifier
 * les tokens JWT et contrôler les autorisations.
 *
 * <p>Les tokens doivent contenir :
 * un émetteur valide, une audience autorisée
 * et un identifiant de sujet non vide.
 */
@Configuration
public class SecurityConfig {

    /** Audience attendue dans les tokens JWT. */
    private static final String API_AUDIENCE =
            "guide-touristique-api";

    /** Rôles autorisés dans l'application. */
    private static final Set<String> ALLOWED_ROLES =
            Set.of("USER", "GUIDE", "ADMIN");

    /**
     * Configure la validation des tokens JWT Keycloak.
     *
     * @param issuer adresse de l'émetteur Keycloak
     * @param jwks adresse des clés publiques Keycloak
     * @return décodeur JWT configuré
     */
    @Bean
    JwtDecoder jwtDecoder(
            @Value(
                "${spring.security.oauth2.resourceserver.jwt.issuer-uri}"
            ) final String issuer,
            @Value(
                "${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}"
            ) final String jwks) {

        var decoder = NimbusJwtDecoder
                .withJwkSetUri(jwks)
                .build();

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(issuer),
                        new JwtClaimValidator<List<String>>(
                                "aud",
                                aud -> aud != null
                                        && aud.contains(API_AUDIENCE)
                        ),
                        new JwtClaimValidator<String>(
                                "sub",
                                sub -> sub != null
                                        && !sub.isBlank()
                        )
                )
        );

        return decoder;
    }

    /**
     * Extrait les rôles autorisés depuis un token JWT.
     *
     * <p>Les rôles sont récupérés depuis
     * realm_access.roles et convertis au format ROLE_.
     *
     * @param jwt token JWT reçu
     * @return collection des autorités Spring Security
     */
    public static Collection<GrantedAuthority> realmAuthorities(
            final Jwt jwt) {

        List<GrantedAuthority> result = new ArrayList<>();

        Map<String, Object> realm =
                jwt.getClaimAsMap("realm_access");

        if (realm != null
                && realm.get("roles") instanceof Collection<?> roles) {

            for (Object role : roles) {
                if (role instanceof String name
                        && ALLOWED_ROLES.contains(name)) {

                    result.add(
                            new SimpleGrantedAuthority("ROLE_" + name)
                    );
                }
            }
        }

        return result;
    }

    /**
     * Configure la conversion des rôles Keycloak.
     *
     * @return convertisseur d'authentification JWT
     */
    static JwtAuthenticationConverter realmRoles() {

        var converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                SecurityConfig::realmAuthorities
        );

        return converter;
    }

    /**
     * Configure les autorisations des endpoints REST.
     *
     * <p>Les routes publiques restent accessibles
     * sans authentification. Les opérations
     * administratives et les opérations du guide
     * nécessitent le rôle correspondant.
     *
     * @param http configuration Spring Security
     * @return chaîne de filtres de sécurité
     * @throws Exception en cas d'erreur de configuration
     */
    @Bean
    SecurityFilterChain securityFilterChain(
            final HttpSecurity http) throws Exception {

        return http
                .cors(cors -> {
                })
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> auth

                        // Documentation et santé du service.
                        .requestMatchers(
                                "/actuator/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Requêtes CORS preflight.
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // Routes réservées à l'administrateur.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/guides/admin"
                        ).hasRole("ADMIN")

                        // Routes du guide authentifié.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/guides/me"
                        ).hasRole("GUIDE")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/guides/me"
                        ).hasRole("GUIDE")

                        // Consultation publique des guides.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/guides",
                                "/api/guides/*"
                        ).permitAll()

                        // Gestion des guides par l'administrateur.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/guides"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/guides/*"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/guides/*"
                        ).hasRole("ADMIN")

                        // Route de traitement des erreurs.
                        .requestMatchers(
                                "/error"
                        ).permitAll()

                        // Consultation publique des photos.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/guides/*/photo"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.HEAD,
                                "/api/guides/*/photo"
                        ).permitAll()

                        // Modification de la photo personnelle.
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/guides/me/photo"
                        ).hasRole("GUIDE")

                        // Suppression de la photo personnelle.
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/guides/me/photo"
                        ).hasRole("GUIDE")

                        // Refuser les routes non autorisées.
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth ->
                        oauth.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        realmRoles()
                                )
                        )
                )
                .build();
    }

    /**
     * Configure les échanges CORS avec Angular.
     *
     * @param origin origine autorisée du frontend
     * @return configuration CORS du microservice
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value(
                "${app.cors.allowed-origin:http://localhost:4200}"
            ) final String origin) {

        var cors = new CorsConfiguration();

        cors.setAllowedOrigins(List.of(origin));

        cors.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        cors.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept"
                )
        );

        var source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", cors);

        return source;
    }
}