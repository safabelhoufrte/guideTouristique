package ma.tourisme.circuit.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/** The only outgoing integration: HTTP/JSON, never another module's tables. */
@Component
public class GuideClient {
  private final RestClient http;

  /** REST client of the Guide service, with short timeouts (2 s connect, 3 s read). */
  public GuideClient(@Value("${services.guide-url}") String guideUrl) {
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(2000);
    factory.setReadTimeout(3000);
    this.http = RestClient.builder().baseUrl(guideUrl).requestFactory(factory).build();
  }

  /**
   * Checks that the guide exists and is available: 400 if not, 503 if Guide does not answer.
   * Only the required fields are read; extra fields from the Guide service are ignored.
   */
  public void requireExisting(long guideId) {
    JsonNode guide;
    try {
      guide = http.get().uri("/api/guides/{id}", guideId).retrieve().body(JsonNode.class);
    } catch (RestClientResponseException error) {
      if (error.getStatusCode().value() == 404) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Guide introuvable");
      }
      throw unavailable();
    } catch (RestClientException error) {
      throw unavailable();
    }
    if (guide == null
        || !guide.isObject()
        || !guide.path("id").isIntegralNumber()
        || !guide.path("id").canConvertToLong()
        || guide.path("id").longValue() != guideId
        || !guide.path("disponible").isBoolean()) {
      throw unavailable();
    }
    // An unavailable guide still exists; Reservation checks booking availability.
  }

  /**
   * Resolves the Guide profile of the caller. Guide owns the link between a Keycloak account and a
   * guideId, so Circuit forwards the caller's own token instead of trusting a client-sent id.
   */
  public long currentGuideId(String bearerToken) {
    JsonNode me;
    try {
      me =
          http.get()
              .uri("/api/guides/me")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
              .retrieve()
              .body(JsonNode.class);
    } catch (RestClientResponseException error) {
      int status = error.getStatusCode().value();
      if (status == 401 || status == 403 || status == 404) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "Aucun profil guide lié à ce compte");
      }
      throw unavailable();
    } catch (RestClientException error) {
      throw unavailable();
    }
    if (me == null
        || !me.isObject()
        || !me.path("id").isIntegralNumber()
        || !me.path("id").canConvertToLong()) {
      throw unavailable();
    }
    return me.path("id").longValue();
  }

  private ResponseStatusException unavailable() {
    return new ResponseStatusException(
        HttpStatus.SERVICE_UNAVAILABLE,
        "Guide API indisponible ou réponse non conforme au contrat");
  }
}
