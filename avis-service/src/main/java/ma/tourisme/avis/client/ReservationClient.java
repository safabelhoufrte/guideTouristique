
package ma.tourisme.avis.client;

// Importation des classes nécessaires pour lire les réponses JSON,
// configurer les requêtes HTTP, gérer les erreurs et utiliser Spring Security.
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;


// @Component indique à Spring que cette classe est un composant
// qu'il doit créer et gérer automatiquement.
@Component
public class ReservationClient {

    // Attribut qui représente le client HTTP utilisé
    // pour communiquer avec le microservice Réservation.
    private final RestClient http;


    // Record permettant de regrouper deux identifiants :
    // circuitId : identifiant du circuit touristique.
    // guideId   : identifiant du guide touristique.
    public record References(long circuitId, long guideId) {}


    // Constructeur de la classe.
    // @Value récupère l'URL du service Réservation depuis application.properties.
    public ReservationClient(@Value("${services.reservation-url}") String url) {

        // Création d'une configuration pour les requêtes HTTP.
        var factory = new SimpleClientHttpRequestFactory();

        // Définition du délai maximum pour établir une connexion : 2 secondes.
        factory.setConnectTimeout(2000);

        // Définition du délai maximum d'attente de la réponse : 3 secondes.
        factory.setReadTimeout(3000);

        // Construction du client HTTP avec :
        // - l'URL principale du service Réservation ;
        // - les paramètres de connexion et de lecture configurés.
        this.http = RestClient.builder()
                .baseUrl(url)
                .requestFactory(factory)
                .build();
    }


    // Méthode qui vérifie si une réservation permet au client
    // de publier un avis.
    // reservationId : identifiant de la réservation à vérifier.
    // jwt : token d'authentification de l'utilisateur.
    // Retourne les identifiants du circuit et du guide si la réservation est éligible.
    public References requireEligible(long reservationId, Jwt jwt) {

        // Variable qui va contenir la réponse JSON du service Réservation.
        JsonNode response;

        try {

            // Envoi d'une requête HTTP GET au service Réservation.
            response = http.get()

                    // Construction de l'URL avec l'identifiant de réservation.
                    // Exemple : /api/reservations/15/eligibilite
                    .uri("/api/reservations/{id}/eligibilite", reservationId)

                    // Ajout du token JWT dans l'en-tête Authorization.
                    // Il permet au service distant d'identifier l'utilisateur.
                    .header("Authorization", "Bearer " + jwt.getTokenValue())

                    // Récupération de la réponse du service.
                    .retrieve()

                    // Conversion du corps de la réponse en objet JsonNode.
                    .body(JsonNode.class);


        // Gestion des erreurs HTTP retournées par le service distant.
        } catch (RestClientResponseException ex) {

            // Si le serveur retourne 403 :
            // cela signifie que l'utilisateur n'a pas le droit
            // d'accéder à cette réservation.
            if (ex.getStatusCode().value() == 403) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Cette réservation ne vous appartient pas."
                );
            }

            // Si le serveur retourne 404 :
            // cela signifie que la réservation demandée est introuvable.
            // Le code transforme cette erreur en HTTP 400.
            if (ex.getStatusCode().value() == 404) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Réservation introuvable."
                );
            }

            // Pour les autres erreurs HTTP,
            // on considère que le service est indisponible.
            throw unavailable();


        // Gestion des erreurs de communication :
        // problème de connexion, délai dépassé, serveur inaccessible, etc.
        } catch (RestClientException ex) {

            // Déclenche une erreur HTTP 503.
            throw unavailable();
        }


        // Vérification de la validité des données reçues dans la réponse JSON.
        if (response == null

                // Vérifie que le champ "eligible" existe sous forme booléenne.
                || !response.path("eligible").isBoolean()

                // Vérifie que l'identifiant reçu correspond à celui demandé.
                || response.path("reservationId").asLong() != reservationId

                // Vérifie que circuitId est un nombre entier.
                || !response.path("circuitId").isIntegralNumber()

                // Vérifie que circuitId est supérieur ou égal à 1.
                || response.path("circuitId").asLong() < 1

                // Vérifie que guideId est un nombre entier.
                || !response.path("guideId").isIntegralNumber()

                // Vérifie que guideId est supérieur ou égal à 1.
                || response.path("guideId").asLong() < 1) {

            // Si une seule vérification échoue,
            // on considère la réponse comme incorrecte.
            throw unavailable();
        }


        // Vérification de l'éligibilité de la réservation.
        // Si eligible vaut false, le client ne peut pas publier un avis.
        if (!response.path("eligible").asBoolean()) {

            // Déclenche une erreur HTTP 409 Conflict.
            // Un avis est autorisé uniquement après une visite acceptée.
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Un avis est possible uniquement après la date d’une visite acceptée."
            );
        }


        // Si toutes les vérifications réussissent,
        // on retourne les identifiants du circuit et du guide.
        return new References(
                response.path("circuitId").asLong(),
                response.path("guideId").asLong()
        );
    }


    // Méthode privée utilisée pour centraliser la gestion
    // des erreurs lorsque le service Réservation est indisponible
    // ou lorsque sa réponse est incorrecte.
    private ResponseStatusException unavailable() {

        // Retourne une exception HTTP 503 Service Unavailable.
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Le service Réservation est momentanément indisponible."
        );
    }
}
