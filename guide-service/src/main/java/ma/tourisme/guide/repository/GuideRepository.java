package ma.tourisme.guide.repository;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import ma.tourisme.guide.model.Guide;
import ma.tourisme.guide.dto.*;
@Repository
public class GuideRepository {
  private final JdbcTemplate jdbc;
  public GuideRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
  private final RowMapper<Guide> mapper = (rs,n) -> new Guide(rs.getLong("id"),rs.getString("nom"),
    rs.getString("ville"),Arrays.asList((String[])rs.getArray("langues").getArray()),
    rs.getBigDecimal("tarif_par_jour"),rs.getBoolean("disponible"),rs.getObject("owner_subject",UUID.class),
    rs.getString("email"),rs.getString("telephone"),rs.getBoolean("actif"));
  public List<Guide> list(String ville,String langue,boolean admin) {
    return jdbc.query("SELECT * FROM guides WHERE actif AND (?='' OR lower(ville)=lower(?)) AND (?='' OR EXISTS(SELECT 1 FROM unnest(langues) l WHERE lower(l)=lower(?))) ORDER BY id",mapper,
      ville==null?"":ville.trim(),ville==null?"":ville.trim(),langue==null?"":langue.trim(),langue==null?"":langue.trim());
  }
  public Optional<Guide> find(long id) {
    return jdbc.query("SELECT * FROM guides WHERE id=? AND actif",mapper,id).stream().findFirst();
  }
  public Optional<Guide> mine(String sub) {
    return jdbc.query("SELECT * FROM guides WHERE owner_subject=?::uuid AND actif",mapper,sub).stream().findFirst();
  }
  public Guide save(Long id,GuideRequest r) {
    var rows = jdbc.query(conn -> {
      var ps=conn.prepareStatement(id==null
        ? "INSERT INTO guides(nom,ville,langues,tarif_par_jour,disponible,owner_subject,email,telephone) VALUES(?,?,?,?,?,?,?,?) RETURNING *"
        : "UPDATE guides SET nom=?,ville=?,langues=?,tarif_par_jour=?,disponible=?,owner_subject=?,email=?,telephone=? WHERE id=? AND actif RETURNING *");
      ps.setString(1,r.nom().trim());ps.setString(2,r.ville().trim());
      ps.setArray(3,conn.createArrayOf("text",r.langues().toArray(String[]::new)));
      ps.setBigDecimal(4,r.tarifParJour());ps.setBoolean(5,r.disponible());ps.setObject(6,r.ownerSubject());
      ps.setString(7,r.email());ps.setString(8,r.telephone());if(id!=null)ps.setLong(9,id);
      return ps;
    },mapper);
    return rows.isEmpty()?null:rows.getFirst();
  }
  public Guide updateMine(String sub,ProfileRequest r) {
    var rows=jdbc.query(conn -> {
      var ps=conn.prepareStatement("UPDATE guides SET nom=?,ville=?,langues=?,disponible=?,email=?,telephone=? WHERE owner_subject=?::uuid AND actif RETURNING *");
      ps.setString(1,r.nom().trim());ps.setString(2,r.ville().trim());ps.setArray(3,conn.createArrayOf("text",r.langues().toArray(String[]::new)));
      ps.setBoolean(4,r.disponible());ps.setString(5,r.email());ps.setString(6,r.telephone());ps.setString(7,sub);return ps;
    },mapper);
    return rows.isEmpty()?null:rows.getFirst();
  }
  public boolean archive(long id) {
    return jdbc.update("UPDATE guides SET actif=FALSE, disponible=FALSE WHERE id=? AND actif",id)==1;
  }
}
