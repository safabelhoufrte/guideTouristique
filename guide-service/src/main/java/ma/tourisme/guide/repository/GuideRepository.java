
package ma.tourisme.guide.repository;

import java.util.List;
import java.util.Optional;

import ma.tourisme.guide.model.Guide;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository responsable de l'accès aux données des guides.
 *
 * <p>Il fournit les opérations CRUD et les méthodes
 * de recherche par ville, langue et compte Keycloak.
 */
public interface GuideRepository extends JpaRepository<Guide, Long> {

    /**
     * Recherche tous les guides actifs.
     *
     * @return liste des guides actifs
     */
    List<Guide> findByActifTrue();

    /**
     * Recherche un guide associé à un compte Keycloak.
     *
     * @param ownerSubject identifiant Keycloak
     * @return guide associé, si présent
     */
    Optional<Guide> findByOwnerSubject(String ownerSubject);

    /**
     * Vérifie si un compte Keycloak est déjà associé.
     *
     * @param ownerSubject identifiant Keycloak
     * @return true si le compte est associé à un guide
     */
    boolean existsByOwnerSubject(String ownerSubject);

    /**
     * Vérifie l'association Keycloak à un autre guide.
     *
     * @param ownerSubject identifiant Keycloak
     * @param id identifiant du guide à exclure
     * @return true si un autre guide possède ce compte
     */
    boolean existsByOwnerSubjectAndIdNot(
            String ownerSubject,
            Long id
    );

    /**
     * Recherche les guides actifs dans une ville donnée.
     *
     * @param ville ville recherchée
     * @return liste des guides correspondants
     */
    List<Guide> findByActifTrueAndVilleIgnoreCase(String ville);

    /**
     * Recherche les guides actifs parlant une langue.
     *
     * @param langue langue recherchée
     * @return liste des guides correspondants
     */
    @Query(
            "SELECT DISTINCT g FROM Guide g JOIN g.langues l "
                    + "WHERE g.actif = true "
                    + "AND LOWER(l) = LOWER(:langue)"
    )
    List<Guide> findActifsParLangue(
            @Param("langue") String langue
    );

    /**
     * Recherche les guides actifs par ville et langue.
     *
     * @param ville ville recherchée
     * @param langue langue recherchée
     * @return liste des guides correspondants
     */
    @Query(
            "SELECT DISTINCT g FROM Guide g JOIN g.langues l "
                    + "WHERE g.actif = true "
                    + "AND LOWER(g.ville) = LOWER(:ville) "
                    + "AND LOWER(l) = LOWER(:langue)"
    )
    List<Guide> findActifsParVilleEtLangue(
            @Param("ville") String ville,
            @Param("langue") String langue
    );

    /**
     * Recherche les guides actifs selon les filtres fournis.
     *
     * <p>Les filtres peuvent être absents ou combinés.
     *
     * @param ville ville recherchée ou null
     * @param langue langue recherchée ou null
     * @return liste des guides correspondants
     */
    default List<Guide> rechercherGuides(
            final String ville,
            final String langue) {

        if (ville == null && langue == null) {
            return findByActifTrue();
        }

        if (ville != null && langue == null) {
            return findByActifTrueAndVilleIgnoreCase(ville);
        }

        if (ville == null) {
            return findActifsParLangue(langue);
        }

        return findActifsParVilleEtLangue(ville, langue);
    }
}
