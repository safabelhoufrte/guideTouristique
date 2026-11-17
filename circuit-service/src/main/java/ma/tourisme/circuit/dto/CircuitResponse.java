package ma.tourisme.circuit.dto;

import java.math.BigDecimal;
import ma.tourisme.circuit.model.Circuit;

public record CircuitResponse(
    long id,
    String titre,
    String description,
    String ville,
    Integer dureeHeures,
    BigDecimal prix,
    long guideId,
    String image) {
  public static CircuitResponse from(Circuit circuit) {
    return new CircuitResponse(
        circuit.id(),
        circuit.titre(),
        circuit.description(),
        circuit.ville(),
        circuit.dureeHeures(),
        circuit.prix(),
        circuit.guideId(),
        circuit.image());
  }
}
