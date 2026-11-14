
package ma.tourisme.guide.repository;

import ma.tourisme.guide.model.Guide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GuideRepository extends JpaRepository<Guide, Long> {

    List<Guide> findByActifTrue();

    @Query("""
        SELECT DISTINCT g
        FROM Guide g
        LEFT JOIN g.langues l
        WHERE g.actif = true
        AND (
            CAST(:ville AS string) IS NULL
            OR LOWER(g.ville) = LOWER(CAST(:ville AS string))
        )
        AND (
            CAST(:langue AS string) IS NULL
            OR LOWER(l) = LOWER(CAST(:langue AS string))
        )
        """)
    List<Guide> rechercherGuides(
            @Param("ville") String ville,
            @Param("langue") String langue
    );
}
