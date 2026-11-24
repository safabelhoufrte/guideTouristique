package ma.tourisme.reservation.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CircuitClient {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record CircuitDto(long id, String titre, BigDecimal prix, long guideId) {}

  private final RestClient http;

  public CircuitClient(@Value("${services.circuit-url}") String url) {
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(2000);
    factory.setReadTimeout(3000);
    this.http = RestClient.builder().baseUrl(url).requestFactory(factory).build();
  }

  public CircuitDto get(long id) {
    try {
      CircuitDto c = http.get().uri("/api/circuits/{id}", id).retrieve().body(CircuitDto.class);
      if (c == null || c.prix() == null) throw unavailable();
      return c;
    } catch (RestClientResponseException e) {
      if (e.getStatusCode().value() == 404) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Circuit introuvable");
      }
      throw unavailable();
    } catch (RestClientException e) {
      throw unavailable();
    }
  }

  private ResponseStatusException unavailable() {
    return new ResponseStatusException(
        HttpStatus.SERVICE_UNAVAILABLE, "Circuit API indisponible ou réponse non conforme");
  }
}