package ma.tourisme.reservation.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class GuideClient {
  private final RestClient http;

  public GuideClient(@Value("${services.guide-url}") String url) {
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(2000);
    factory.setReadTimeout(3000);
    this.http = RestClient.builder().baseUrl(url).requestFactory(factory).build();
  }


/**
 * Récupère le véritable ID du guide connecté
 * depuis le service Guide, avec son JWT Keycloak.
 */
  public Long getAuthenticatedGuideId(String accessToken) {

    JsonNode response;

    try {
        response = http.get()
            .uri("/api/guides/me")
            .headers(headers ->
                headers.setBearerAuth(accessToken)
            )
            .retrieve()
            .body(JsonNode.class);

    } catch (RestClientResponseException e) {

        if (e.getStatusCode().value() == 401) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Token Keycloak invalide"
            );
        }

        if (e.getStatusCode().value() == 403) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acces Guide refuse"
            );
        }

        if (e.getStatusCode().value() == 404) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Aucun profil Guide associe"
            );
        }

        throw unavailable();

    } catch (RestClientException e) {
        throw unavailable();
    }

    if (response == null ||
        !response.path("id").canConvertToLong()) {

        throw unavailable();
    }

    return response.path("id").longValue();
}
  
  /** Refuse la réservation si le guide n'existe pas ou n'est pas disponible. */
  public void requireAvailable(long guideId) {
    JsonNode guide;
    try {
      guide = http.get().uri("/api/guides/{id}", guideId).retrieve().body(JsonNode.class);
    } catch (RestClientResponseException e) {
      if (e.getStatusCode().value() == 404) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Guide introuvable");
      }
      throw unavailable();
    } catch (RestClientException e) {
      throw unavailable();
    }
    if (guide == null || !guide.path("disponible").isBoolean()) throw unavailable();
    if (!guide.path("disponible").booleanValue()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Guide non disponible");
    }
  }

  private ResponseStatusException unavailable() {
    return new ResponseStatusException(
        HttpStatus.SERVICE_UNAVAILABLE, "Guide API indisponible ou réponse non conforme");
  }
}