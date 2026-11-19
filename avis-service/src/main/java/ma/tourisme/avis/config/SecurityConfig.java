
package ma.tourisme.avis.config;

// Importation des collections Java : List, Map, Set, Collection, ArrayList...
import java.util.*;

// Importation des annotations et composants Spring.
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Importation des outils de sécurité Spring.
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

// Importation des classes pour vérifier les tokens JWT.
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;

// Permet de transformer les rôles du JWT en autorités Spring Security.
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

// Permet de configurer les règles de sécurité HTTP.
import org.springframework.security.web.SecurityFilterChain;

// Importation des classes nécessaires à la configuration CORS.
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


// @Configuration indique à Spring que cette classe contient
// des configurations qui doivent être utilisées par l'application.
@Configuration
public class SecurityConfig {


    // =========================================================
    // PARTIE 1 : CONFIGURATION ET VALIDATION DU TOKEN JWT
    // =========================================================

    // @Bean indique à Spring de créer et gérer automatiquement
    // l'objet retourné par cette méthode.
    //
    // JwtDecoder permet de décoder et de vérifier les tokens JWT.
    //
    // issuer : adresse du serveur qui a délivré le token.
    // jwks : adresse contenant les clés publiques utilisées
    // pour vérifier la signature du token.
    @Bean
    JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwks) {

        // Création du décodeur JWT en utilisant l'adresse JWKS.
        // Il permet de vérifier la signature du token.
        var decoder = NimbusJwtDecoder.withJwkSetUri(jwks).build();

        // Configuration des règles de validation du token JWT.
        // Toutes les validations doivent réussir pour accepter le token.
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(

                // Vérifie les conditions standards du JWT,
                // notamment l'émetteur (issuer) et les dates de validité.
                JwtValidators.createDefaultWithIssuer(issuer),

                // Vérifie que le claim "aud" contient
                // l'identifiant de notre API.
                new JwtClaimValidator<List<String>>(
                        "aud",
                        aud -> aud != null && aud.contains("guide-touristique-api")
                ),

                // Vérifie que le claim "sub" existe
                // et qu'il n'est pas vide.
                new JwtClaimValidator<String>(
                        "sub",
                        sub -> sub != null && !sub.isBlank()
                )
        ));

        // Retourne le décodeur JWT configuré.
        return decoder;
    }


    // =========================================================
    // PARTIE 2 : CONFIGURATION DES RÈGLES DE SÉCURITÉ HTTP
    // =========================================================

    // @Bean permet à Spring de gérer cette configuration.
    //
    // SecurityFilterChain définit les règles qui déterminent
    // quelles requêtes HTTP sont autorisées ou interdites.
    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {

        // Création d'un convertisseur permettant de transformer
        // les informations du token JWT en rôles Spring Security.
        var converter = new JwtAuthenticationConverter();

        // Récupération et conversion des rôles présents dans le JWT.
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {

            // Liste qui va contenir les autorisations de l'utilisateur.
            List<GrantedAuthority> result = new ArrayList<>();

            // Récupération du contenu du claim "realm_access".
            // Dans ce projet, il contient les rôles de l'utilisateur.
            Map<String, Object> realm = jwt.getClaimAsMap("realm_access");

            // Vérifie que realm_access existe et que son champ "roles"
            // contient une collection de rôles.
            if (realm != null && realm.get("roles") instanceof Collection<?> roles) {

                // Parcours de tous les rôles récupérés depuis le JWT.
                for (Object role : roles) {

                    // On accepte uniquement les trois rôles autorisés :
                    // USER, GUIDE et ADMIN.
                    // On ajoute le préfixe ROLE_ exigé par hasRole() de Spring.
                    if (role instanceof String name
                            && Set.of("USER", "GUIDE", "ADMIN").contains(name)) {

                        result.add(new SimpleGrantedAuthority("ROLE_" + name));
                    }
                }
            }

            // Retourne la liste des rôles et autorisations de l'utilisateur.
            return result;
        });


        // Construction de la chaîne de filtres de sécurité.
        return http

                // Activation de la configuration CORS.
                // Elle permet de contrôler les origines qui peuvent
                // communiquer avec notre API.
                .cors(cors -> {})

                // Désactivation de CSRF.
                // Adapté ici à une API utilisant des tokens JWT
                // et non une authentification basée sur les sessions navigateur.
                .csrf(csrf -> csrf.disable())

                // Configuration de la gestion des sessions.
                // STATELESS signifie que le serveur ne conserve
                // pas de session utilisateur entre les requêtes.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Définition des règles d'accès aux différentes URL.
                .authorizeHttpRequests(auth -> auth

                        // Ces routes sont accessibles sans authentification :
                        // santé du service et documentation Swagger.
                        .requestMatchers(
                                "/actuator/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Autorise toutes les requêtes OPTIONS.
                        // Elles sont notamment utilisées pour les vérifications CORS.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Tout le monde peut consulter la moyenne des avis.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/avis/moyenne"
                        ).permitAll()

                        // Seul un ADMIN peut consulter cette route.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/avis/admin"
                        ).hasRole("ADMIN")

                        // Seul un utilisateur USER peut consulter ses propres avis.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/avis/me"
                        ).hasRole("USER")

                        // Tout le monde peut consulter les avis publics.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/avis",
                                "/api/avis/*"
                        ).permitAll()

                        // Seul un USER peut créer un avis.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/avis"
                        ).hasRole("USER")

                        // Seul un ADMIN peut modifier la publication d'un avis.
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/avis/*/publication"
                        ).hasRole("ADMIN")

                        // Seul un ADMIN peut supprimer un avis.
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/avis/*"
                        ).hasRole("ADMIN")

                        // Toute autre route commençant par /api/avis/
                        // nécessite le rôle ADMIN.
                        .requestMatchers("/api/avis/**").hasRole("ADMIN")

                        // Toutes les autres requêtes sont refusées.
                        // Cela évite d'autoriser accidentellement une nouvelle route.
                        .anyRequest().denyAll()
                )

                // Activation de l'authentification OAuth2 Resource Server
                // avec JWT et utilisation du convertisseur de rôles configuré.
                .oauth2ResourceServer(oauth ->
                        oauth.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(converter)
                        )
                )

                // Construit et retourne la configuration finale de sécurité.
                .build();
    }


    // =========================================================
    // PARTIE 3 : CONFIGURATION CORS
    // =========================================================

    // @Bean permet à Spring de gérer la configuration CORS.
    //
    // CorsConfigurationSource définit les règles qui contrôlent
    // les communications entre le frontend et le backend.
    @Bean
    CorsConfigurationSource corsConfigurationSource() {

        // Création d'un objet pour définir les règles CORS.
        var cors = new CorsConfiguration();

        // Autorise uniquement le frontend qui fonctionne
        // à cette adresse : http://localhost:4200.
        cors.setAllowedOrigins(List.of("http://localhost:4200"));

        // Définition des méthodes HTTP autorisées.
        // GET : consulter.
        // POST : créer.
        // PUT : remplacer.
        // PATCH : modifier partiellement.
        // DELETE : supprimer.
        // OPTIONS : vérifier les autorisations CORS.
        cors.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );

        // Définition des en-têtes HTTP autorisés.
        // Authorization : transmettre le token JWT.
        // Content-Type : préciser le format des données envoyées.
        cors.setAllowedHeaders(
                List.of("Authorization", "Content-Type")
        );

        // Création d'une source de configuration CORS
        // basée sur les URL.
        var source = new UrlBasedCorsConfigurationSource();

        // Applique les règles CORS à toutes les routes de l'API.
        source.registerCorsConfiguration("/**", cors);

        // Retourne la configuration CORS.
        return source;
    }
}
