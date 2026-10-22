package ma.tourisme.circuit.model;

import java.math.BigDecimal;

/** Internal model; HTTP serialization belongs to the DTOs. */
public record Circuit(long id, String titre, BigDecimal prix, long guideId, boolean disponible) {}
