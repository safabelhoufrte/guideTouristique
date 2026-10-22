package ma.tourisme.circuit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import ma.tourisme.circuit.model.Circuit;

public record CircuitResponse(
    long id,
    String titre,
    BigDecimal prix,
    @JsonProperty("guide_id") long guideId,
    boolean disponible) {
  public static CircuitResponse from(Circuit circuit) {
    return new CircuitResponse(
        circuit.id(), circuit.titre(), circuit.prix(), circuit.guideId(), circuit.disponible());
  }
}
