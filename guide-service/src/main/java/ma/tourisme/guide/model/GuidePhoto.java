package ma.tourisme.guide.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Représente la photo associée à un guide touristique.
 *
 * <p>Les données de l'image sont conservées dans PostgreSQL
 * sous forme binaire dans la table guide_photo.
 */
@Entity
@Table(name = "guide_photo")
public class GuidePhoto {

    /** Longueur maximale du type MIME de la photo. */
    private static final int CONTENT_TYPE_LENGTH = 20;

    /** Identifiant du guide propriétaire de la photo. */
    @Id
    @Column(name = "guide_id")
    private Long guideId;

    /** Type MIME du fichier image. */
    @Column(
            name = "content_type",
            nullable = false,
            length = CONTENT_TYPE_LENGTH
    )
    private String contentType;

    /** Contenu binaire de la photo stockée dans PostgreSQL. */
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(
            name = "data",
            nullable = false,
            columnDefinition = "bytea"
    )
    private byte[] data;

    /**
     * Constructeur sans argument requis par JPA.
     */
    public GuidePhoto() {
    }

    /**
     * Initialise une photo de guide.
     *
     * @param nouvelId identifiant du guide
     * @param nouveauType type MIME de l'image
     * @param nouvellesDonnees contenu binaire de l'image
     */
    public GuidePhoto(
            final Long nouvelId,
            final String nouveauType,
            final byte[] nouvellesDonnees) {

        this.guideId = nouvelId;
        this.contentType = nouveauType;
        this.data = nouvellesDonnees;
    }

    /**
     * Retourne l'identifiant du guide.
     *
     * @return identifiant du guide
     */
    public Long getGuideId() {
        return guideId;
    }

    /**
     * Modifie l'identifiant du guide.
     *
     * @param nouvelId nouvel identifiant
     */
    public void setGuideId(final Long nouvelId) {
        this.guideId = nouvelId;
    }

    /**
     * Retourne le type MIME de la photo.
     *
     * @return type MIME
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Modifie le type MIME de la photo.
     *
     * @param nouveauType nouveau type MIME
     */
    public void setContentType(final String nouveauType) {
        this.contentType = nouveauType;
    }

    /**
     * Retourne les données binaires de la photo.
     *
     * @return contenu binaire de l'image
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Modifie les données binaires de la photo.
     *
     * @param nouvellesDonnees nouveau contenu binaire
     */
    public void setData(final byte[] nouvellesDonnees) {
        this.data = nouvellesDonnees;
    }
}