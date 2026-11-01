package ma.tourisme.guide.dto;
import java.math.BigDecimal;
import java.util.List;
import ma.tourisme.guide.model.Guide;
/** Public directory information; no account subject or private contact information. */
public record GuideResponse(long id, String nom, String ville, List<String> langues,
  BigDecimal tarifParJour, boolean disponible) {
  public static GuideResponse from(Guide g) {
    return new GuideResponse(g.id(),g.nom(),g.ville(),g.langues(),g.tarifParJour(),g.disponible());
  }
}
