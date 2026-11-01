package ma.tourisme.avis.dto;
import jakarta.validation.constraints.*;
public record AvisRequest(@NotNull @Positive Long reservationId,
  @NotNull @Min(1) @Max(5) Integer noteGuide,@NotNull @Min(1) @Max(5) Integer noteCircuit,
  @NotBlank @Size(max=2000) String commentaire){}
