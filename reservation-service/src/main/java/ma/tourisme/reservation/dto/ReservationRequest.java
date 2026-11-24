package ma.tourisme.reservation.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record ReservationRequest(
    @NotNull Long circuitId,
    @NotNull @FutureOrPresent LocalDate dateVisite,
    @Min(1) int nbPersonnes,
    @NotBlank String nom,
    @NotBlank @Email String email,
    @Size(max = 40) String codePromo) {

  /** Constructeur sans code promo. */
  public ReservationRequest(Long circuitId, LocalDate dateVisite, int nbPersonnes, String nom, String email) {
    this(circuitId, dateVisite, nbPersonnes, nom, email, null);
  }
}