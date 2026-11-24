
package ma.tourisme.guide.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Représente les données reçues pour créer ou modifier un guide.
 *
 * <p>Les champs ownerSubject et disponible sont réservés
 * aux opérations administratives.
 *
 * @param nom nom du guide
 * @param prenom prénom du guide
 * @param ville ville d'activité
 * @param langues langues parlées par le guide
 * @param email adresse électronique
 * @param telephone numéro de téléphone
 * @param tarifParJour tarif journalier
 * @param ownerSubject identifiant Keycloak du guide
 * @param disponible disponibilité du guide
 */
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
                @NotBlank(
                        message = "Une langue ne peut pas etre vide"
                )
                @Size(max = 50)
                String
        > langues,

        @Email(message = "Adresse email invalide")
        @Size(max = 150)
        String email,

        @Size(max = 30)
        String telephone,

        @PositiveOrZero(
                message = "Le tarif doit etre positif ou nul"
        )
        double tarifParJour,

        // Association Keycloak réservée à l'administrateur.
        @Pattern(
                regexp = "^[0-9a-fA-F]{8}"
                        + "(-[0-9a-fA-F]{4}){3}"
                        + "-[0-9a-fA-F]{12}$",
                message = "Le compte doit etre "
                        + "un identifiant Keycloak (UUID)"
        )
        String ownerSubject,

        // Disponibilité modifiable par l'administrateur.
        Boolean disponible

) {

    /**
     * Construit une requête contenant uniquement
     * les informations du profil Guide.
     *
     * <p>L'association Keycloak et la disponibilité
     * ne sont pas modifiées par ce constructeur.
     *
     * @param nouveauNom nom du guide
     * @param nouveauPrenom prénom du guide
     * @param nouvelleVille ville d'activité
     * @param nouvellesLangues langues parlées
     * @param nouvelEmail adresse électronique
     * @param nouveauTelephone numéro de téléphone
     * @param nouveauTarif tarif journalier
     */
    public GuideRequest(
            final String nouveauNom,
            final String nouveauPrenom,
            final String nouvelleVille,
            final List<String> nouvellesLangues,
            final String nouvelEmail,
            final String nouveauTelephone,
            final double nouveauTarif) {

        this(
                nouveauNom,
                nouveauPrenom,
                nouvelleVille,
                nouvellesLangues,
                nouvelEmail,
                nouveauTelephone,
                nouveauTarif,
                null,
                null
        );
    }
}
