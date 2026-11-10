package ma.tourisme.guide.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

public record GuideRequest(
        @NotBlank(message = "Le nom est obligatoire") String nom,
        String prenom,
        String ville,
        List<String> langues,
        @Email(message = "Email invalide") String email,
        String telephone,
        @PositiveOrZero(message = "Le tarif doit être positif ou nul") double tarifParJour) {}