package ma.tourisme.reservation.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "circuit_id", nullable = false)
    private Long circuitId;

    @Column(name = "guide_id", nullable = false)
    private Long guideId;

    @Column(name = "date_visite", nullable = false)
    private LocalDate dateVisite;

    @Column(name = "nb_personnes", nullable = false)
    private Integer nbPersonnes;

    @Column(nullable = false, length = 160)
    private String nom;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(name = "prix_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixTotal;

    @Column(nullable = false, length = 20)
    private String statut;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "code_promo", length = 40)
    private String codePromo;

    @Column(name = "reduction_pct", nullable = false)
    private Integer reductionPct = 0;

    @PrePersist
    void avantInsertion() {
        if (dateCreation == null) dateCreation = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCircuitId() { return circuitId; }
    public void setCircuitId(Long circuitId) { this.circuitId = circuitId; }

    public Long getGuideId() { return guideId; }
    public void setGuideId(Long guideId) { this.guideId = guideId; }

    public LocalDate getDateVisite() { return dateVisite; }
    public void setDateVisite(LocalDate dateVisite) { this.dateVisite = dateVisite; }

    public Integer getNbPersonnes() { return nbPersonnes; }
    public void setNbPersonnes(Integer nbPersonnes) { this.nbPersonnes = nbPersonnes; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public BigDecimal getPrixTotal() { return prixTotal; }
    public void setPrixTotal(BigDecimal prixTotal) { this.prixTotal = prixTotal; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public String getCodePromo() { return codePromo; }
    public void setCodePromo(String codePromo) { this.codePromo = codePromo; }

    public Integer getReductionPct() { return reductionPct; }
    public void setReductionPct(Integer reductionPct) { this.reductionPct = reductionPct; }
}