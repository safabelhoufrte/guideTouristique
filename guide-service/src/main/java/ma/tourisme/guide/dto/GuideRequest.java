
package ma.tourisme.guide.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GuideRequest(

        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
        String nom,

        @Size(max = 100, message = "Le prénom ne doit pas dépasser 100 caractères")
        String prenom,

        @Size(max = 100, message = "La ville ne doit pas dépasser 100 caractères")
        String ville,

        List<String> langues,

        @Email(message = "Adresse email invalide")
        @Size(max = 150, message = "L'email ne doit pas dépasser 150 caractères")
        String email,

        @Size(max = 30, message = "Le téléphone ne doit pas dépasser 30 caractères")
        String telephone,

        @PositiveOrZero(message = "Le tarif doit être positif ou nul")
        double tarifParJour

) {}
