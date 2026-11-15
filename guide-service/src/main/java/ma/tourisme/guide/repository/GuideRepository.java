package ma.tourisme.guide.repository;

import ma.tourisme.guide.model.Guide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GuideRepository extends JpaRepository<Guide, Long> {

    List<Guide> findByActifTrue();

    Optional<Guide> findByOwnerSubject(String ownerSubject);

    boolean existsByOwnerSubject(String ownerSubject);

    boolean existsByOwnerSubjectAndIdNot(String ownerSubject, Long id);

    List<Guide> findByActifTrueAndVilleIgnoreCase(String ville);

    @Query("SELECT DISTINCT g FROM Guide g JOIN g.langues l " +
           "WHERE g.actif = true AND LOWER(l) = LOWER(:langue)")
    List<Guide> findActifsParLangue(
            @Param("langue") String langue
    );

    @Query("SELECT DISTINCT g FROM Guide g JOIN g.langues l " +
           "WHERE g.actif = true " +
           "AND LOWER(g.ville) = LOWER(:ville) " +
           "AND LOWER(l) = LOWER(:langue)")
    List<Guide> findActifsParVilleEtLangue(
            @Param("ville") String ville,
            @Param("langue") String langue
    );

    default List<Guide> rechercherGuides(String ville, String langue) {

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