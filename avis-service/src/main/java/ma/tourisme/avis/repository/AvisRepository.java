package ma.tourisme.avis.repository;

import java.util.List;
import java.util.Optional;
import ma.tourisme.avis.client.ReservationClient.References;
import ma.tourisme.avis.dto.AvisRequest;
import ma.tourisme.avis.dto.AvisResponse;
import ma.tourisme.avis.dto.MoyenneResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AvisRepository {
  private final JdbcTemplate jdbc;

  public AvisRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private final RowMapper<AvisResponse> mapper = (rs, rowNum) -> new AvisResponse(
      rs.getLong("id"),
      rs.getLong("reservation_id"),
      rs.getLong("circuit_id"),
      rs.getLong("guide_id"),
      rs.getString("auteur"),
      rs.getInt("note_guide"),
      rs.getInt("note_circuit"),
      rs.getString("commentaire"),
      rs.getTimestamp("date_creation").toLocalDateTime(),
      rs.getBoolean("publie")
  );

  public List<AvisResponse> list(Long circuitId, Long guideId, boolean admin) {
    return jdbc.query(
        "SELECT * FROM avis WHERE (publie OR ?) "
            + "AND (?::bigint IS NULL OR circuit_id = ?) "
            + "AND (?::bigint IS NULL OR guide_id = ?) "
            + "ORDER BY date_creation DESC",
        mapper,
        admin,
        circuitId,
        circuitId,
        guideId,
        guideId
    );
  }

  public Optional<AvisResponse> findById(long id) {
    return jdbc.query("SELECT * FROM avis WHERE id = ?", mapper, id).stream().findFirst();
  }

  public List<AvisResponse> mine(String sub) {
    return jdbc.query(
        "SELECT * FROM avis WHERE user_subject = ? ORDER BY date_creation DESC",
        mapper,
        sub
    );
  }

  public AvisResponse insert(AvisRequest r, References refs, String sub, String auteur) {
    return jdbc.queryForObject(
        "INSERT INTO avis(reservation_id, circuit_id, guide_id, user_subject, auteur, note_guide, note_circuit, commentaire) "
            + "VALUES(?, ?, ?, ?, ?, ?, ?, ?) RETURNING *",
        mapper,
        r.reservationId(),
        refs.circuitId(),
        refs.guideId(),
        sub,
        auteur,
        r.noteGuide(),
        r.noteCircuit(),
        r.commentaire().trim()
    );
  }

  public Optional<AvisResponse> publication(long id, boolean publie) {
    return jdbc.query(
        "UPDATE avis SET publie = ? WHERE id = ? RETURNING *",
        mapper,
        publie,
        id
    ).stream().findFirst();
  }

  public boolean delete(long id) {
    return jdbc.update("DELETE FROM avis WHERE id = ?", id) > 0;
  }

  public MoyenneResponse calculerMoyenneCircuit(long circuitId) {
    return jdbc.queryForObject(
        "SELECT COALESCE(ROUND(AVG(note_circuit)::numeric, 2), 0.0) AS moyenne, COUNT(*) AS nombre "
            + "FROM avis WHERE circuit_id = ? AND publie = true",
        (rs, n) -> new MoyenneResponse(
            "CIRCUIT",
            circuitId,
            rs.getDouble("moyenne"),
            rs.getLong("nombre")
        ),
        circuitId
    );
  }

  public MoyenneResponse calculerMoyenneGuide(long guideId) {
    return jdbc.queryForObject(
        "SELECT COALESCE(ROUND(AVG(note_guide)::numeric, 2), 0.0) AS moyenne, COUNT(*) AS nombre "
            + "FROM avis WHERE guide_id = ? AND publie = true",
        (rs, n) -> new MoyenneResponse(
            "GUIDE",
            guideId,
            rs.getDouble("moyenne"),
            rs.getLong("nombre")
        ),
        guideId
    );
  }
}
