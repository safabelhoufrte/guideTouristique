package ma.tourisme.reservation.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PromoRequest(
    @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{3,40}$") String code,
    @Min(1) @Max(100) int pourcentage,
    @Min(1) int personnesMin,
    boolean premiereReservationSeulement,
    boolean bienvenue,
    LocalDate dateExpiration,
    @Min(1) Integer utilisationsMax) {}