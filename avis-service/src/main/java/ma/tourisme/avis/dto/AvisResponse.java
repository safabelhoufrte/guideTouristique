package ma.tourisme.avis.dto;
import java.time.LocalDateTime;
public record AvisResponse(long id,long reservationId,long circuitId,long guideId,String auteur,
  int noteGuide,int noteCircuit,String commentaire,LocalDateTime dateCreation,boolean publie){}
