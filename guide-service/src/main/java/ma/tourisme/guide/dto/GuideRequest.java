package ma.tourisme.guide.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
public record GuideRequest(
  @NotBlank @Size(max=160) String nom,
  @NotBlank @Size(max=120) String ville,
  @NotEmpty @Size(max=20) List<@NotBlank @Size(max=40) String> langues,
  @NotNull @DecimalMin("0.00") @Digits(integer=8,fraction=2) BigDecimal tarifParJour,
  @NotNull Boolean disponible, UUID ownerSubject,
  @Email @Size(max=160) String email, @Size(max=40) String telephone) {}
