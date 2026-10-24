package ma.tourisme.circuit.repository;

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
        row.getString("description"),
        row.getString("ville"),
        row.getObject("duree_heures", Integer.class),
        row.getBigDecimal("prix"),
        row.getLong("guide_id"));
  }

  public List<Circuit> findAll(String ville, Long guideId) {
    return db.query(
        """
        SELECT * FROM circuit.circuits WHERE disponible=TRUE
        AND (CAST(? AS VARCHAR) IS NULL OR ville=?)
        AND (CAST(? AS BIGINT) IS NULL OR guide_id=?) ORDER BY id
        """,
        this::map,
        ville,
        ville,
        guideId,
        guideId);
  }

  public Optional<Circuit> findById(long id) {
    return db
        .query("SELECT * FROM circuit.circuits WHERE id=? AND disponible=TRUE", this::map, id)
        .stream()
        .findFirst();
  }

  public Circuit insert(Circuit circuit) {
    return db.queryForObject(
        """
        INSERT INTO circuit.circuits(titre,description,ville,duree_heures,prix,guide_id,disponible)
        VALUES (?,?,?,?,?,?,TRUE) RETURNING *
        """,
        this::map,
        circuit.titre(),
        circuit.description(),
        circuit.ville(),
        circuit.dureeHeures(),
        circuit.prix(),
        circuit.guideId());
  }

  public Optional<Circuit> update(Circuit circuit) {
    return db
        .query(
            """
            UPDATE circuit.circuits SET titre=?,description=?,ville=?,duree_heures=?,prix=?,guide_id=?
            WHERE id=? AND disponible=TRUE RETURNING *
            """,
            this::map,
            circuit.titre(),
            circuit.description(),
            circuit.ville(),
            circuit.dureeHeures(),
            circuit.prix(),
            circuit.guideId(),
            circuit.id())
        .stream()
        .findFirst();
  }

  // Keep historical records while deleted circuits disappear from the public API.
  public boolean archive(long id) {
    return db.update(
            "UPDATE circuit.circuits SET disponible=FALSE WHERE id=? AND disponible=TRUE", id)
        > 0;
  }
}
