package ma.tourisme.circuit.model;

import java.math.BigDecimal;

/** A circuit as stored in the circuit schema ({@code image}: data URL, link, or null). */
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
      long id,
      String titre,
      String description,
      String ville,
      Integer dureeHeures,
      BigDecimal prix,
      long guideId) {
    this(id, titre, description, ville, dureeHeures, prix, guideId, null);
  }
}
