
package ma.tourisme.guide.dto;

import java.util.List;

/**
 * Représente les informations d'un guide
 * destinées à l'espace administrateur.
 *
 * <p>Cette réponse inclut les guides actifs et inactifs,
 * ainsi que leur éventuel compte Keycloak associé.
 *
 * <p>Elle ne doit pas être retournée par les API publiques.
 *
 * @param id identifiant du guide
 * @param nom nom du guide
 * @param prenom prénom du guide
 * @param ville ville d'activité du guide
 * @param langues langues parlées par le guide
 * @param email adresse électronique du guide
 * @param telephone numéro de téléphone du guide
 * @param tarifParJour tarif journalier du guide
 * @param disponible disponibilité du guide
 * @param actif statut actif du guide
 * @param ownerSubject identifiant du compte Keycloak associé
 * @param noteMoyenne note moyenne du guide
 */
public record GuideAdminResponse(

        Long id,

        String nom,

        String prenom,

        String ville,

        List<String> langues,

        String email,

        String telephone,

        double tarifParJour,

        boolean disponible,

        boolean actif,

        String ownerSubject,

        Double noteMoyenne

) {
}
