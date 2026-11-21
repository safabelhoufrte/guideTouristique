
package db.migration.guide;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V7__seed_demo_guides_with_photos
        extends BaseJavaMigration {

    record Demo(
        String nom,
        String prenom,
        String ville,
        String email,
        String telephone,
        double tarif,
        boolean actif,
        List<String> langues,
        String fichier
    ) {}

    private static final List<Demo> GUIDES = List.of(
        new Demo(
            "Benali", "Youssef", "Agadir",
            "youssef@example.com", "0600000000",
            400, true,
            List.of("français", "anglais", "arabe"),
            "youssef-benali.jpg"
        ),
        new Demo(
            "Alaoui", "Salma", "Marrakech",
            "salma@example.com", "0600000001",
            350, true,
            List.of("français", "arabe"),
            "salma-alaoui.jpg"
        ),
        new Demo(
            "Idrissi", "Omar", "Agadir",
            "omar@example.com", "0600000002",
            450, false,
            List.of("anglais", "espagnol"),
            "omar-idrissi.jpg"
        ),
        new Demo(
            "Ouazzani", "Khadija", "Essaouira",
            "khadija@example.com", "0600000003",
            380, true,
            List.of("français", "anglais", "arabe"),
            "khadija-ouazzani.jpg"
        ),
        new Demo(
            "El Fassi", "Amine", "Fès",
            "amine@example.com", "0600000004",
            320, true,
            List.of("français", "arabe", "espagnol"),
            "amine-el-fassi.jpg"
        ),
        new Demo(
            "Berrada", "Yasmine", "Marrakech",
            "yasmine@example.com", "0600000005",
            420, true,
            List.of("français", "anglais"),
            "yasmine-berrada.jpg"
        )
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection db = context.getConnection();

        for (Demo demo : GUIDES) {

            Long id = chercherId(db, demo.email());

            // Créer uniquement les guides manquants.
            if (id == null) {
                try (PreparedStatement ps = db.prepareStatement("""
                    INSERT INTO guide.guide
                    (nom, prenom, ville, email, telephone,
                     tarif_par_jour, actif)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """)) {

                    ps.setString(1, demo.nom());
                    ps.setString(2, demo.prenom());
                    ps.setString(3, demo.ville());
                    ps.setString(4, demo.email());
                    ps.setString(5, demo.telephone());
                    ps.setDouble(6, demo.tarif());
                    ps.setBoolean(7, demo.actif());
                    ps.executeUpdate();
                }

                id = chercherId(db, demo.email());

                if (id == null) {
                    throw new IllegalStateException(
                        "Guide non créé : " + demo.email()
                    );
                }

                // Ajouter les langues uniquement au guide créé.
                for (String langue : demo.langues()) {
                    try (PreparedStatement ps =
                            db.prepareStatement("""
                                INSERT INTO guide.guide_langues
                                (guide_id, langue)
                                VALUES (?, ?)
                                ON CONFLICT DO NOTHING
                                """)) {

                        ps.setLong(1, id);
                        ps.setString(2, langue);
                        ps.executeUpdate();
                    }
                }
            }

            // Ne jamais remplacer une photo existante.
            if (!photoExiste(db, id)) {
                String chemin =
                    "/db/demo/guide/" + demo.fichier();

                try (InputStream input =
                        getClass().getResourceAsStream(chemin)) {

                    if (input == null) {
                        throw new IllegalStateException(
                            "Photo absente : " + chemin
                        );
                    }

                    byte[] image = input.readAllBytes();

                    try (PreparedStatement ps =
                            db.prepareStatement("""
                                INSERT INTO guide.guide_photo
                                (guide_id, content_type, data)
                                VALUES (?, ?, ?)
                                ON CONFLICT (guide_id) DO NOTHING
                                """)) {

                        ps.setLong(1, id);
                        ps.setString(2, "image/jpeg");
                        ps.setBytes(3, image);
                        ps.executeUpdate();
                    }
                }
            }
        }
    }

    private Long chercherId(Connection db, String email)
            throws Exception {

        try (PreparedStatement ps =
                db.prepareStatement("""
                    SELECT id
                    FROM guide.guide
                    WHERE email = ?
                    """)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    private boolean photoExiste(Connection db, Long guideId)
            throws Exception {

        try (PreparedStatement ps =
                db.prepareStatement("""
                    SELECT 1
                    FROM guide.guide_photo
                    WHERE guide_id = ?
                    """)) {

            ps.setLong(1, guideId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
