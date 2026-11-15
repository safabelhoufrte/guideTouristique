package ma.tourisme.circuit.model;

import java.math.BigDecimal;

public record Circuit(
    long id,
    String titre,
    String description,
    String ville,
    Integer dureeHeures,
    BigDecimal prix,
    long guideId) {}
