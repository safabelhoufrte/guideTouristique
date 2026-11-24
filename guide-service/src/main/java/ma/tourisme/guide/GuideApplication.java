
package ma.tourisme.guide;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée du microservice Guide.
 *
 * <p>Initialise et démarre l'application Spring Boot.
 */
@SpringBootApplication
public class GuideApplication {

    /**
     * Démarre le microservice Guide.
     *
     * @param args arguments de lancement
     */
    public static void main(final String[] args) {
        SpringApplication.run(GuideApplication.class, args);
    }
}