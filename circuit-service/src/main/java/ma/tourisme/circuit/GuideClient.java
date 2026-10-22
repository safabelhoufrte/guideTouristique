package ma.tourisme.circuit;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/**
 * The only outgoing integration: HTTP/JSON, never another module's database.
 */
@Component
public class GuideClient {
    private final RestClient http;

    public GuideClient(@Value("${services.guide-url}") String guideUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.http = RestClient.builder().baseUrl(guideUrl).requestFactory(factory).build();
    }

    // Read only required wire fields; extra teammate fields are harmless.
    public void requireActive(long guideId) {
        JsonNode guide;
        try {
            guide = http.get().uri("/guides/{id}", guideId).retrieve().body(JsonNode.class);
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 404) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Guide introuvable");
            }
            throw unavailable();
        } catch (RestClientException error) {
            throw unavailable();
        }
        if (guide == null || !guide.isObject()
                || !guide.path("id").isIntegralNumber() || !guide.path("id").canConvertToLong()
                || guide.path("id").longValue() != guideId || !guide.path("actif").isBoolean()) {
            throw unavailable();
        }
        if (!guide.path("actif").booleanValue()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Guide archivé");
        }
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Guide API indisponible ou réponse non conforme au contrat");
    }
}
