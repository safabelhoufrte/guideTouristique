package ma.tourisme.circuit.repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import ma.tourisme.circuit.model.Circuit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CircuitRepository {
  private final JdbcTemplate db;

  public CircuitRepository(JdbcTemplate db) {
    this.db = db;
  }

  private Circuit map(ResultSet row, int index) throws SQLException {
    return new Circuit(
        row.getLong("id"),
        row.getString("titre"),
        row.getBigDecimal("prix"),
        row.getLong("guide_id"),
        row.getBoolean("disponible"));
  }

  public List<Circuit> findAll(Long guideId) {
    return guideId == null
        ? db.query("SELECT * FROM circuit.circuits ORDER BY id", this::map)
        : db.query(
            "SELECT * FROM circuit.circuits WHERE guide_id=? ORDER BY id", this::map, guideId);
  }

  public Optional<Circuit> findById(long id) {
    return db.query("SELECT * FROM circuit.circuits WHERE id=?", this::map, id).stream()
        .findFirst();
  }

  public Circuit insert(String titre, BigDecimal prix, long guideId, boolean disponible) {
    return db.queryForObject(
        """
        INSERT INTO circuit.circuits(titre, prix, guide_id, disponible)
        VALUES (?, ?, ?, ?) RETURNING *
        """,
        this::map,
        titre,
        prix,
        guideId,
        disponible);
  }

  public Optional<Circuit> update(
      long id, String titre, BigDecimal prix, long guideId, boolean disponible) {
    return db
        .query(
            """
            UPDATE circuit.circuits SET titre=?, prix=?, guide_id=?, disponible=?
            WHERE id=? RETURNING *
            """,
            this::map,
            titre,
            prix,
            guideId,
            disponible,
            id)
        .stream()
        .findFirst();
  }

  public boolean archive(long id) {
    return db.update("UPDATE circuit.circuits SET disponible=FALSE WHERE id=?", id) > 0;
  }
}
