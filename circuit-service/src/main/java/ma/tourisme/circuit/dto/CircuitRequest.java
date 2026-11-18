package ma.tourisme.circuit.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CircuitRequest(
    @NotBlank @Size(max = 160) String titre,
    @NotBlank @Size(max = 4000) String description,
    @NotBlank @Size(max = 120) String ville,
    @NotNull @Positive Integer dureeHeures,
    @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal prix,
    @NotNull @Positive Long guideId,
    // null = keep the current photo (PUT) / no photo (POST); "" = remove it; else the new photo.
    @Size(max = 1500000) @Pattern(regexp = "^$|^data:image/(jpeg|png|webp);base64,.*") String image) {

  /** Constructeur sans photo. */
  public CircuitRequest(
      String titre, String description, String ville, Integer dureeHeures, BigDecimal prix, Long guideId) {
    this(titre, description, ville, dureeHeures, prix, guideId, null);
  }
}
