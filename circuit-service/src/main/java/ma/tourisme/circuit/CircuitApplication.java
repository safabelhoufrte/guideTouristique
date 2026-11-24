package ma.tourisme.circuit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Circuit microservice: circuits catalogue of Guide Touristique (port 8082). */
@SpringBootApplication
public class CircuitApplication {
  /** Starts the Spring Boot application. */
  public static void main(String[] args) {
    SpringApplication.run(CircuitApplication.class, args);
  }
}
