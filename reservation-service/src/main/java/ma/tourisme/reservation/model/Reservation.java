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

    @PrePersist
    void avantInsertion() {
        if (dateCreation == null) dateCreation = LocalDateTime.now();
    }

    // + getters et setters pour tous les champs
}