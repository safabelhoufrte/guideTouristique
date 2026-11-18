package db.migration.circuit;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Base64;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Demo circuits for a fresh installation, with their photos.
 *
 * <p>Circuits are linked to the guides seeded by guide-service (V2__seed_guides.sql): 1 = Youssef
 * Benali (Agadir), 2 = Salma Alaoui (Marrakech). Guide 3 is inactive, so it gets no circuit. A
 * database that already has circuits is left unchanged.
 *
 * <p>Photos come from {@code db/demo/circuit/} and are stored as data URLs, like photos uploaded
 * from the admin editor. Sources: docs/landing-photos.md.
 */
public class V4__seed_demo_circuits extends BaseJavaMigration {
  record Demo(
      String titre, String description, String ville, int dureeHeures, String prix, long guideId,
      String photo) {}

  static final List<Demo> CIRCUITS =
      List.of(
          new Demo(
              "Médina et Kasbah d'Agadir",
              "Montée à la Kasbah Agadir Oufella pour le panorama sur la baie, puis visite de la"
                  + " Médina de Coco Polizzi et de ses ateliers d'artisans.",
              "Agadir", 4, "250.00", 1, "kasbah-agadir.jpg"),
          new Demo(
              "Souk El Had et front de mer",
              "Découverte du Souk El Had, ses épices et produits locaux, suivie d'une promenade"
                  + " guidée sur la corniche jusqu'à la marina.",
              "Agadir", 3, "180.00", 1, "souk-el-had.jpg"),
          new Demo(
              "Vallée du Paradis",
              "Excursion d'une journée dans l'arrière-pays d'Agadir : palmeraies, gorges et"
                  + " piscines naturelles de la Vallée du Paradis.",
              "Agadir", 8, "450.00", 1, "vallee-du-paradis.jpg"),
          new Demo(
              "Médina de Marrakech",
              "Parcours à pied de la place Jemaa el-Fna aux souks, en passant par la médersa Ben"
                  + " Youssef et les fondouks historiques.",
              "Marrakech", 4, "300.00", 2, "medina-marrakech.jpg"),
          new Demo(
              "Jardins de Marrakech",
              "Visite du Jardin Majorelle et du Jardin Secret, avec l'histoire de leurs plantes, de"
                  + " leur architecture et de leurs créateurs.",
              "Marrakech", 3, "220.00", 2, "jardins-marrakech.jpg"));

  @Override
  public void migrate(Context context) throws Exception {
    var connection = context.getConnection();
    try (Statement check = connection.createStatement();
        ResultSet existing = check.executeQuery("SELECT EXISTS (SELECT 1 FROM circuit.circuits)")) {
      existing.next();
      if (existing.getBoolean(1)) return;
    }
    try (PreparedStatement insert =
        connection.prepareStatement(
            "INSERT INTO circuit.circuits"
                + " (titre, description, ville, duree_heures, prix, guide_id, image, disponible)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)")) {
      for (Demo demo : CIRCUITS) {
        insert.setString(1, demo.titre());
        insert.setString(2, demo.description());
        insert.setString(3, demo.ville());
        insert.setInt(4, demo.dureeHeures());
        insert.setBigDecimal(5, new BigDecimal(demo.prix()));
        insert.setLong(6, demo.guideId());
        insert.setString(7, photo(demo.photo()));
        insert.addBatch();
      }
      insert.executeBatch();
    }
  }

  static String photo(String file) throws IOException {
    try (InputStream in =
        V4__seed_demo_circuits.class.getResourceAsStream("/db/demo/circuit/" + file)) {
      if (in == null) throw new IllegalStateException("Photo de démo manquante : " + file);
      return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(in.readAllBytes());
    }
  }
}
