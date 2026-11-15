
package ma.tourisme.guide.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    @Size(max = 30)
    List<
        @NotBlank(message = "Une langue ne peut pas etre vide")
        @Size(max = 50)
        String
    > langues,

    @Email(message = "Adresse email invalide")
    @Size(max = 150)
    String email,

    @Size(max = 30)
    String telephone,

    @PositiveOrZero(message = "Le tarif doit etre positif ou nul")
    double tarifParJour,

    // Admin only (ignored on PUT /api/guides/me): Keycloak account UUID, or null for none.
    @Pattern(regexp = "^[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$",
             message = "Le compte doit etre un identifiant Keycloak (UUID)")
    String ownerSubject,

    // Admin only: availability shown to travellers (null keeps the current value).
    Boolean disponible

) {
    /** Profile fields only (no account link, availability unchanged). */
    public GuideRequest(String nom, String prenom, String ville, List<String> langues,
                        String email, String telephone, double tarifParJour) {
        this(nom, prenom, ville, langues, email, telephone, tarifParJour, null, null);
    }
}