
package ma.tourisme.guide.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            GuideJwtFilter jwtFilter) throws Exception {

        return http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()

                .requestMatchers(
                    HttpMethod.POST, "/api/guide-auth/login")
                    .permitAll()

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/guides/me")
                    .hasRole("GUIDE")

                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/guides/me")
                    .hasRole("GUIDE")

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/guides", "/api/guides/*")
                    .permitAll()

                .requestMatchers("/actuator/health")
                    .permitAll()

                .anyRequest().denyAll()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, error) ->
                    res.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED))
                .accessDeniedHandler((req, res, error) ->
                    res.sendError(
                        HttpServletResponse.SC_FORBIDDEN))
            )
            .addFilterBefore(
                jwtFilter,
                UsernamePasswordAuthenticationFilter.class
            )
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origin:http://localhost:4200}")
            String origin) {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origin));
        config.setAllowedMethods(
            List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(
            List.of("Authorization", "Content-Type", "Accept"));

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/api/**", config);

        return source;
    }
}