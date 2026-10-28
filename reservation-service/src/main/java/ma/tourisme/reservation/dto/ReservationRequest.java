package ma.tourisme.reservation.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record ReservationRequest(
    @NotNull Long circuitId,
    @NotNull @FutureOrPresent LocalDate dateVisite,
    @Min(1) int nbPersonnes,
    @NotBlank String nom,
    @NotBlank @Email String email) {}