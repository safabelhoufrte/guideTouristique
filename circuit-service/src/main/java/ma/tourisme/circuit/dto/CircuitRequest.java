package ma.tourisme.circuit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CircuitRequest(
    @NotBlank @Size(max = 160) String titre,
    @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal prix,
    @JsonProperty("guide_id") @NotNull @Positive Long guideId,
    @NotNull Boolean disponible) {}
