
package ma.tourisme.avis.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ReservationClient {

    private final RestClient http;

    public record References(long circuitId, long guideId) {}

    public record ReservationSummary(long id, long circuitId, long guideId, String statut, LocalDate dateVisite, String email) {
        public ReservationSummary(long id, long circuitId, long guideId, String statut, LocalDate dateVisite) {
            this(id, circuitId, guideId, statut, dateVisite, null);
        }
    }

    public ReservationClient(@Value("${services.reservation-url}") String url) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.http = RestClient.builder()
                .baseUrl(url)
                .requestFactory(factory)
                .build();
    }

    public static boolean isEligible(ReservationSummary r, String expectedEmail) {
        if (r == null) {
            return false;
        }
        if (!"ACCEPTEE".equalsIgnoreCase(r.statut())) {
            return false;
        }
        if (r.dateVisite() == null || !r.dateVisite().isBefore(LocalDate.now())) {
            return false;
        }
        if (expectedEmail != null && !expectedEmail.isBlank() && r.email() != null && !r.email().isBlank()) {
            if (!expectedEmail.trim().equalsIgnoreCase(r.email().trim())) {
                return false;
            }
        }
        return true;
    }

    public References requireEligible(long reservationId, Jwt jwt) {
        String email = jwt != null ? jwt.getClaimAsString("email") : null;
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Email manquant dans le token."
            );
        }

        List<ReservationSummary> userReservations = findUserReservations(jwt);

        ReservationSummary reservation = userReservations.stream()
                .filter(r -> r.id() == reservationId)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Cette réservation ne vous appartient pas ou est introuvable."
                ));

        if (reservation.email() != null && !reservation.email().isBlank()
                && !email.trim().equalsIgnoreCase(reservation.email().trim())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Cette réservation ne vous appartient pas."
            );
        }

        if (!isEligible(reservation, email)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Un avis est possible uniquement après la date d’une visite acceptée."
            );
        }

        return new References(reservation.circuitId(), reservation.guideId());
    }

    public List<ReservationSummary> findUserReservations(Jwt jwt) {
        String email = jwt != null ? jwt.getClaimAsString("email") : null;
        if (email == null || email.isBlank()) {
            return List.of();
        }
        JsonNode response;
        try {
            response = http.get()
                    .uri("/api/reservations?email={email}", email)
                    .header("Authorization", "Bearer " + jwt.getTokenValue())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw unavailable();
        }

        if (response == null || !response.isArray()) {
            throw unavailable();
        }

        List<ReservationSummary> list = new ArrayList<>();
        for (JsonNode node : response) {
            if (!node.isObject()) {
                throw unavailable();
            }
            long id = node.path("id").asLong(-1);
            long circuitId = node.path("circuitId").asLong(-1);
            long guideId = node.path("guideId").asLong(-1);
            if (id <= 0 || circuitId <= 0 || guideId <= 0) {
                throw unavailable();
            }
            String statut = node.path("statut").asText(null);
            String dateStr = node.path("dateVisite").asText(null);
            LocalDate dateVisite = null;
            if (dateStr != null && !dateStr.isBlank()) {
                try {
                    dateVisite = LocalDate.parse(dateStr.length() >= 10 ? dateStr.substring(0, 10) : dateStr);
                } catch (Exception ex) {
                    throw unavailable();
                }
            }
            String resEmail = node.has("email") && !node.path("email").isNull()
                    ? node.path("email").asText()
                    : null;

            list.add(new ReservationSummary(id, circuitId, guideId, statut, dateVisite, resEmail));
        }
        return list;
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Le service Réservation est momentanément indisponible."
        );
    }
}
