package ma.tourisme.avis.repository;
import java.util.*;
import ma.tourisme.avis.dto.*;
import ma.tourisme.avis.client.ReservationClient.References;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
@Repository
public class AvisRepository {
  private final JdbcTemplate jdbc;
  public AvisRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
  private final RowMapper<AvisResponse> mapper=(rs,n)->new AvisResponse(rs.getLong("id"),rs.getLong("reservation_id"),rs.getLong("circuit_id"),rs.getLong("guide_id"),rs.getString("auteur"),rs.getInt("note_guide"),rs.getInt("note_circuit"),rs.getString("commentaire"),rs.getTimestamp("date_creation").toLocalDateTime(),rs.getBoolean("publie"));
  public List<AvisResponse> list(Long circuitId,Long guideId,boolean admin){
    return jdbc.query("SELECT * FROM avis WHERE (publie OR ?) AND (?::bigint IS NULL OR circuit_id=?) AND (?::bigint IS NULL OR guide_id=?) ORDER BY date_creation DESC",mapper,admin,circuitId,circuitId,guideId,guideId);
  }
  public List<AvisResponse> mine(String sub){return jdbc.query("SELECT * FROM avis WHERE user_subject=? ORDER BY date_creation DESC",mapper,sub);}
  public AvisResponse insert(AvisRequest r,References refs,String sub,String auteur){
    return jdbc.queryForObject("INSERT INTO avis(reservation_id,circuit_id,guide_id,user_subject,auteur,note_guide,note_circuit,commentaire) VALUES(?,?,?,?,?,?,?,?) RETURNING *",mapper,
      r.reservationId(),refs.circuitId(),refs.guideId(),sub,auteur,r.noteGuide(),r.noteCircuit(),r.commentaire().trim());
  }
  public Optional<AvisResponse> publication(long id,boolean publie){return jdbc.query("UPDATE avis SET publie=? WHERE id=? RETURNING *",mapper,publie,id).stream().findFirst();}
}
