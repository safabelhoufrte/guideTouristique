package ma.tourisme.guide.client;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
@Component
public class CircuitClient {
  private final RestClient http;
  public CircuitClient(@Value("${services.circuit-url:http://localhost:8082}")String url) {
    var f=new SimpleClientHttpRequestFactory();f.setConnectTimeout(2000);f.setReadTimeout(3000);
    http=RestClient.builder().baseUrl(url).requestFactory(f).build();
  }
  public JsonNode assigned(long guideId) {
    try {
      var rows=http.get().uri("/api/circuits?guideId={id}",guideId).retrieve().body(JsonNode.class);
      if(rows==null||!rows.isArray())throw new RestClientException("Invalid circuits");
      for(var row:rows)if(!row.path("guideId").isIntegralNumber()||row.path("guideId").longValue()!=guideId)throw new RestClientException("Invalid guide reference");
      return rows;
    }catch(RestClientException ex){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Les circuits sont momentanément indisponibles.");}
  }
}
