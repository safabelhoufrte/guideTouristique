package ma.tourisme.guide.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente un guide touristique enregistré dans l'application.
 *
 * <p>Cette entité contient les informations personnelles,
 * professionnelles et les langues parlées par un guide.
 */
@Entity
@Table(name = "guide")
public class Guide {

    /** Longueur maximale de l'identifiant Keycloak. */
    private static final int OWNER_SUBJECT_LENGTH = 36;

    /** Identifiant unique du guide. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nom du guide. */
    private String nom;

    /** Prénom du guide. */
    private String prenom;

    /** Ville d'activité du guide. */
    private String ville;

    /** Adresse électronique du guide. */
    private String email;

    /** Numéro de téléphone du guide. */
    private String telephone;

    /** Tarif journalier du guide. */
    @Column(name = "tarif_par_jour")
    private double tarifParJour;

    /** Indique si le guide est actif. */
    @Column(name = "actif")
    private boolean actif = true;

    /** Note moyenne attribuée au guide. */
    @Column(name = "note_moyenne")
    private Double noteMoyenne;

    /**
     * Identifiant du compte Keycloak associé au guide.
     * L'association est réalisée par un administrateur.
     */
    @Column(name = "owner_subject", length = OWNER_SUBJECT_LENGTH)
    private String ownerSubject;

    /** Langues parlées par le guide. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "guide_langues",
        joinColumns = @JoinColumn(name = "guide_id")
    )
    @Column(name = "langue")
    private List<String> langues = new ArrayList<>();

    /**
     * Retourne l'identifiant du guide.
     *
     * @return identifiant du guide
     */
    public Long getId() {
        return id;
    }

    /**
     * Retourne le nom du guide.
     *
     * @return nom du guide
     */
    public String getNom() {
        return nom;
    }

    /**
     * Modifie le nom du guide.
     *
     * @param nouveauNom nouveau nom
     */
    public void setNom(final String nouveauNom) {
        this.nom = nouveauNom;
    }

    /**
     * Retourne le prénom du guide.
     *
     * @return prénom du guide
     */
    public String getPrenom() {
        return prenom;
    }

    /**
     * Modifie le prénom du guide.
     *
     * @param nouveauPrenom nouveau prénom
     */
    public void setPrenom(final String nouveauPrenom) {
        this.prenom = nouveauPrenom;
    }

    /**
     * Retourne la ville du guide.
     *
     * @return ville du guide
     */
    public String getVille() {
        return ville;
    }

    /**
     * Modifie la ville du guide.
     *
     * @param nouvelleVille nouvelle ville
     */
    public void setVille(final String nouvelleVille) {
        this.ville = nouvelleVille;
    }

    /**
     * Retourne l'adresse électronique du guide.
     *
     * @return adresse électronique
     */
    public String getEmail() {
        return email;
    }

    /**
     * Modifie l'adresse électronique du guide.
     *
     * @param nouvelEmail nouvelle adresse électronique
     */
    public void setEmail(final String nouvelEmail) {
        this.email = nouvelEmail;
    }

    /**
     * Retourne le numéro de téléphone du guide.
     *
     * @return numéro de téléphone
     */
    public String getTelephone() {
        return telephone;
    }

    /**
     * Modifie le numéro de téléphone du guide.
     *
     * @param nouveauTelephone nouveau numéro de téléphone
     */
    public void setTelephone(final String nouveauTelephone) {
        this.telephone = nouveauTelephone;
    }

    /**
     * Retourne le tarif journalier du guide.
     *
     * @return tarif par jour
     */
    public double getTarifParJour() {
        return tarifParJour;
    }

    /**
     * Modifie le tarif journalier du guide.
     *
     * @param nouveauTarif nouveau tarif par jour
     */
    public void setTarifParJour(final double nouveauTarif) {
        this.tarifParJour = nouveauTarif;
    }

    /**
     * Indique si le guide est actif.
     *
     * @return true si le guide est actif
     */
    public boolean isActif() {
        return actif;
    }

    /**
     * Modifie le statut actif du guide.
     *
     * @param nouvelEtat nouveau statut du guide
     */
    public void setActif(final boolean nouvelEtat) {
        this.actif = nouvelEtat;
    }

    /**
     * Retourne la note moyenne du guide.
     *
     * @return note moyenne ou null si aucune note
     */
    public Double getNoteMoyenne() {
        return noteMoyenne;
    }

    /**
     * Modifie la note moyenne du guide.
     *
     * @param nouvelleNote nouvelle note moyenne
     */
    public void setNoteMoyenne(final Double nouvelleNote) {
        this.noteMoyenne = nouvelleNote;
    }

    /**
     * Retourne les langues parlées par le guide.
     *
     * @return liste des langues
     */
    public List<String> getLangues() {
        return langues;
    }

    /**
     * Modifie les langues parlées par le guide.
     *
     * @param nouvellesLangues nouvelle liste des langues
     */
    public void setLangues(final List<String> nouvellesLangues) {
        this.langues = nouvellesLangues;
    }

    /**
     * Retourne l'identifiant Keycloak associé au guide.
     *
     * @return identifiant Keycloak ou null
     */
    public String getOwnerSubject() {
        return ownerSubject;
    }

    /**
     * Associe un compte Keycloak au guide.
     *
     * @param nouveauSubject identifiant du compte Keycloak
     */
    public void setOwnerSubject(final String nouveauSubject) {
        this.ownerSubject = nouveauSubject;
    }
}
