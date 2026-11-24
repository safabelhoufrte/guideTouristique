package db.migration.guide;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Migration Flyway V7 : insertion des guides de démonstration.
 *
 * <p>Crée uniquement les guides absents et ajoute leurs photos
 * lorsqu'aucune photo n'est déjà enregistrée.
 */
public class V7__seed_demo_guides_with_photos
        extends BaseJavaMigration {

    /** Index JDBC du premier paramètre. */
    private static final int PARAM_ONE = 1;

    /** Index JDBC du deuxième paramètre. */
    private static final int PARAM_TWO = 2;

    /** Index JDBC du troisième paramètre. */
    private static final int PARAM_THREE = 3;

    /** Index JDBC du quatrième paramètre. */
    private static final int PARAM_FOUR = 4;

    /** Index JDBC du cinquième paramètre. */
    private static final int PARAM_FIVE = 5;

    /** Index JDBC du sixième paramètre. */
    private static final int PARAM_SIX = 6;

    /** Index JDBC du septième paramètre. */
    private static final int PARAM_SEVEN = 7;

    /**
     * Données utilisées pour un guide de démonstration.
     *
     * @param nom nom
     * @param prenom prénom
     * @param ville ville
     * @param email adresse électronique
     * @param telephone téléphone
     * @param tarif tarif journalier
     * @param actif statut du guide
     * @param langues langues parlées
     * @param fichier fichier de la photo
     */
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
    ) {
    }

    /** Guides fictifs insérés dans la base de démonstration. */
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

    /**
     * Exécute l'insertion des données de démonstration.
     *
     * @param context contexte de migration Flyway
     * @throws Exception en cas d'erreur SQL ou de lecture
     */
    @Override
    public void migrate(final Context context) throws Exception {

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

                    ps.setString(PARAM_ONE, demo.nom());
                    ps.setString(PARAM_TWO, demo.prenom());
                    ps.setString(PARAM_THREE, demo.ville());
                    ps.setString(PARAM_FOUR, demo.email());
                    ps.setString(PARAM_FIVE, demo.telephone());
                    ps.setDouble(PARAM_SIX, demo.tarif());
                    ps.setBoolean(PARAM_SEVEN, demo.actif());
                    ps.executeUpdate();
                }

                id = chercherId(db, demo.email());

                if (id == null) {
                    throw new IllegalStateException(
                            "Guide non créé : " + demo.email()
                    );
                }

                // Ajouter les langues du nouveau guide.
                for (String langue : demo.langues()) {
                    try (PreparedStatement ps =
                            db.prepareStatement("""
                                    INSERT INTO guide.guide_langues
                                    (guide_id, langue)
                                    VALUES (?, ?)
                                    ON CONFLICT DO NOTHING
                                    """)) {

                        ps.setLong(PARAM_ONE, id);
                        ps.setString(PARAM_TWO, langue);
                        ps.executeUpdate();
                    }
                }
            }

            // Préserver les photos existantes.
            if (!photoExiste(db, id)) {
                String chemin = "/db/demo/guide/" + demo.fichier();

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

                        ps.setLong(PARAM_ONE, id);
                        ps.setString(PARAM_TWO, "image/jpeg");
                        ps.setBytes(PARAM_THREE, image);
                        ps.executeUpdate();
                    }
                }
            }
        }
    }

    /**
     * Recherche un guide par son adresse électronique.
     *
     * @param db connexion à la base
     * @param email adresse électronique recherchée
     * @return identifiant du guide ou null
     * @throws Exception en cas d'erreur SQL
     */
    private Long chercherId(
            final Connection db,
            final String email) throws Exception {

        try (PreparedStatement ps = db.prepareStatement("""
                SELECT id
                FROM guide.guide
                WHERE email = ?
                """)) {

            ps.setString(PARAM_ONE, email);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(PARAM_ONE) : null;
            }
        }
    }

    /**
     * Vérifie la présence d'une photo pour un guide.
     *
     * @param db connexion à la base
     * @param guideId identifiant du guide
     * @return true si la photo existe
     * @throws Exception en cas d'erreur SQL
     */
    private boolean photoExiste(
            final Connection db,
            final Long guideId) throws Exception {

        try (PreparedStatement ps = db.prepareStatement("""
                SELECT 1
                FROM guide.guide_photo
                WHERE guide_id = ?
                """)) {

            ps.setLong(PARAM_ONE, guideId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
