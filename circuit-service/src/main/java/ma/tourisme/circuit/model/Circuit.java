package ma.tourisme.circuit.model;

import java.math.BigDecimal;

public record Circuit(
    long id,
    String titre,
    String description,
    String ville,
    Integer dureeHeures,
    BigDecimal prix,
    long guideId,
    String image) {

  /** Constructeur historique (sans photo). */
  public Circuit(
      long id, String titre, String description, String ville, Integer dureeHeures, BigDecimal prix, long guideId) {
    this(id, titre, description, ville, dureeHeures, prix, guideId, null);
  }
}
