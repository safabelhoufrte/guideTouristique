package ma.tourisme.guide.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GuideRequest(

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100)
    String nom,

    @Size(max = 100)
    String prenom,

    @Size(max = 100)
    String ville,

    List<String> langues,

    @Email(message = "Adresse email invalide")
    @Size(max = 150)
    String email,

    @Size(max = 30)
    String telephone,

    @PositiveOrZero(message = "Le tarif doit être positif ou nul")
    double tarifParJour

) {}
