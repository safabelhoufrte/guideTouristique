package ma.tourisme.circuit.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import ma.tourisme.circuit.model.Circuit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** SQL access to circuit.circuits (JdbcTemplate, parameterized queries only). */
@Repository
public class CircuitRepository {
  /**
   * Columns read for a circuit. The photo itself is never loaded here (it can weigh ~1 MB):
   * only whether one exists, so reads return a link to GET /api/circuits/{id}/image.
   */
  private static final String COLUMNS =
      "id,titre,description,ville,duree_heures,prix,guide_id,(image IS NOT NULL) AS has_image";

  private final JdbcTemplate db;

  /** Creates the repository on the service's own datasource (schema circuit). */
  public CircuitRepository(JdbcTemplate db) {
    this.db = db;
  }

  /** Public link to the photo of circuit {@code id}. */
  public static String imageLink(long id) {
    return "/api/circuits/" + id + "/image";
  }

  private Circuit map(ResultSet row, int index) throws SQLException {
    long id = row.getLong("id");
    return new Circuit(
        id,
        row.getString("titre"),
        row.getString("description"),
        row.getString("ville"),
        row.getObject("duree_heures", Integer.class),
        row.getBigDecimal("prix"),
        row.getLong("guide_id"),
        row.getBoolean("has_image") ? imageLink(id) : null);
  }

  /** Available circuits, optionally filtered by city and/or guide, ordered by id. */
  public List<Circuit> findAll(String ville, Long guideId) {
    return db.query(
        "SELECT " + COLUMNS + " FROM circuit.circuits WHERE disponible=TRUE"
            + " AND (CAST(? AS VARCHAR) IS NULL OR ville=?)"
            + " AND (CAST(? AS BIGINT) IS NULL OR guide_id=?) ORDER BY id",
        this::map,
        ville,
        ville,
        guideId,
        guideId);
  }

  /** One available circuit, empty if missing or archived. */
  public Optional<Circuit> findById(long id) {
    return db
        .query(
            "SELECT " + COLUMNS + " FROM circuit.circuits WHERE id=? AND disponible=TRUE",
            this::map,
            id)
        .stream()
        .findFirst();
  }

  /** The stored photo (a data URL) of an available circuit, if it has one. */
  public Optional<String> findImage(long id) {
    return db
        .queryForList(
            "SELECT image FROM circuit.circuits"
                + " WHERE id=? AND disponible=TRUE AND image IS NOT NULL",
            String.class,
            id)
        .stream()
        .findFirst();
  }

  /** {@code circuit.image()} is a data URL to store, or null / "" for no photo. */
  public Circuit insert(Circuit circuit) {
    String image = circuit.image() == null || circuit.image().isEmpty() ? null : circuit.image();
    return db.queryForObject(
        "INSERT INTO circuit.circuits"
            + "(titre,description,ville,duree_heures,prix,guide_id,image,disponible)"
            + " VALUES (?,?,?,?,?,?,?,TRUE) RETURNING " + COLUMNS,
        this::map,
        circuit.titre(),
        circuit.description(),
        circuit.ville(),
        circuit.dureeHeures(),
        circuit.prix(),
        circuit.guideId(),
        image);
  }

  /**
   * {@code circuit.image()}: null keeps the current photo, "" removes it, a data URL replaces it.
   */
  public Optional<Circuit> update(Circuit circuit) {
    return db
        .query(
            "UPDATE circuit.circuits"
                + " SET titre=?,description=?,ville=?,duree_heures=?,prix=?,guide_id=?,"
                + " image = CASE WHEN CAST(? AS TEXT) IS NULL THEN image"
                + " WHEN CAST(? AS TEXT) = '' THEN NULL ELSE CAST(? AS TEXT) END"
                + " WHERE id=? AND disponible=TRUE RETURNING " + COLUMNS,
            this::map,
            circuit.titre(),
            circuit.description(),
            circuit.ville(),
            circuit.dureeHeures(),
            circuit.prix(),
            circuit.guideId(),
            circuit.image(),
            circuit.image(),
            circuit.image(),
            circuit.id())
        .stream()
        .findFirst();
  }

  /** Archives a circuit: kept for history, hidden from the public API. */
  public boolean archive(long id) {
    return db.update(
            "UPDATE circuit.circuits SET disponible=FALSE WHERE id=? AND disponible=TRUE", id)
        > 0;
  }
}
