package ma.tourisme.reservation.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record ReservationRequest(
    @NotNull @Positive Long circuitId,
    @NotNull @FutureOrPresent LocalDate dateVisite,
    @Min(1) int nbPersonnes,
    @NotBlank @Size(max=160) String nom,
    @NotBlank @Email @Size(max=160) String email) {}
