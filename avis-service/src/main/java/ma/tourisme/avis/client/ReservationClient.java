package ma.tourisme.avis.client;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
@Component
public class ReservationClient {
  private final RestClient http;
  public record References(long circuitId,long guideId){}
  public ReservationClient(@Value("${services.reservation-url}")String url){
    var f=new SimpleClientHttpRequestFactory();f.setConnectTimeout(2000);f.setReadTimeout(3000);
    http=RestClient.builder().baseUrl(url).requestFactory(f).build();
  }
  public References requireEligible(long id,Jwt jwt){
    JsonNode response;
    try {
      response=http.get().uri("/api/reservations/{id}/eligibilite",id)
        .header("Authorization","Bearer "+jwt.getTokenValue()).retrieve().body(JsonNode.class);
    }catch(RestClientResponseException ex){
      if(ex.getStatusCode().value()==403)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Cette réservation ne vous appartient pas.");
      if(ex.getStatusCode().value()==404)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Réservation introuvable.");
      throw unavailable();
    }catch(RestClientException ex){throw unavailable();}
    if(response==null||!response.path("eligible").isBoolean()||response.path("reservationId").asLong()!=id
      ||!response.path("circuitId").isIntegralNumber()||response.path("circuitId").asLong()<1
      ||!response.path("guideId").isIntegralNumber()||response.path("guideId").asLong()<1)throw unavailable();
    if(!response.path("eligible").asBoolean())throw new ResponseStatusException(HttpStatus.CONFLICT,"Un avis est possible après la date d’une visite acceptée.");
    return new References(response.path("circuitId").asLong(),response.path("guideId").asLong());
  }
  private ResponseStatusException unavailable(){return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Le service Réservation est momentanément indisponible.");}
}
