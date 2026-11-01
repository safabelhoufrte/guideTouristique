package ma.tourisme.guide.dto;
import jakarta.validation.constraints.*;
import java.util.List;
/** The account binding and rate are controlled by the administrator, never by this DTO. */
public record ProfileRequest(
  @NotBlank @Size(max=160) String nom,
  @NotBlank @Size(max=120) String ville,
  @NotEmpty @Size(max=20) List<@NotBlank @Size(max=40) String> langues,
  @NotNull Boolean disponible,
  @Email @Size(max=160) String email, @Size(max=40) String telephone) {}
