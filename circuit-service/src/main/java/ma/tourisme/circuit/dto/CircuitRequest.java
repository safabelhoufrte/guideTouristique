package ma.tourisme.circuit.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Body of POST and PUT /api/circuits.
 *
 * <p>{@code image}: null keeps the current photo (PUT) or means no photo (POST), "" removes it,
 * a data URL (JPEG, PNG or WebP, 1.5 MB max) replaces it.
 */
public record CircuitRequest(
    @NotBlank @Size(max = 160) String titre,
    @NotBlank @Size(max = 4000) String description,
    @NotBlank @Size(max = 120) String ville,
    @NotNull @Positive Integer dureeHeures,
    @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal prix,
    @NotNull @Positive Long guideId,
    @Size(max = 1500000) @Pattern(regexp = IMAGE_PATTERN) String image) {

  /** Empty (remove the photo) or a JPEG, PNG or WebP data URL. */
  public static final String IMAGE_PATTERN = "^$|^data:image/(jpeg|png|webp);base64,.*";

  /** Constructeur sans photo. */
  public CircuitRequest(
      String titre,
      String description,
      String ville,
      Integer dureeHeures,
      BigDecimal prix,
      Long guideId) {
    this(titre, description, ville, dureeHeures, prix, guideId, null);
  }
}
