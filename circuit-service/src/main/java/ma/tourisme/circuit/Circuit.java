package ma.tourisme.circuit;

import java.math.BigDecimal;

/** Public JSON response; no teammate needs this Java class to call the API. */
public record Circuit(long id, String titre, BigDecimal prix, long guideId, boolean disponible) {}
