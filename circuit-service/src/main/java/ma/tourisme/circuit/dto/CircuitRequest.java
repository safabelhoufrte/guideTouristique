package ma.tourisme.circuit.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CircuitRequest(
    @NotBlank @Size(max = 160) String titre,
    @NotBlank @Size(max = 4000) String description,
    @NotBlank @Size(max = 120) String ville,
    @NotNull @Positive Integer dureeHeures,
    @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal prix,
    @NotNull @Positive Long guideId) {}
