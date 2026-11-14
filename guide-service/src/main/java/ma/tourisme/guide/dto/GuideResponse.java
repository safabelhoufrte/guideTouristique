
package ma.tourisme.guide.dto;

import java.util.List;

public record GuideResponse(
        Long id,
        String nom,
        String prenom,
        String ville,
        List<String> langues,
        String email,
        String telephone,
        double tarifParJour,
        boolean disponible,
        Double noteMoyenne
) {}
