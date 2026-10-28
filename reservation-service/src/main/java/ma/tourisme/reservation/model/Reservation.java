package ma.tourisme.reservation.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
public class Reservation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long circuitId;
  private Long guideId;
  private LocalDate dateVisite;
  private int nbPersonnes;
  private String nom;
  private String email;
  private BigDecimal prixTotal;

  @Enumerated(EnumType.STRING)
  private StatutReservation statut;

  private LocalDateTime dateCreation;
}