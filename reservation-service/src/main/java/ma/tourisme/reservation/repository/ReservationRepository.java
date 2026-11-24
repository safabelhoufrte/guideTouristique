package ma.tourisme.reservation.repository;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.tourisme.reservation.model.Reservation;
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByEmail(String email);
    List<Reservation> findByStatut(String statut);
    List<Reservation> findByCircuitIdAndDateVisite(Long circuitId, LocalDate dateVisite);
    List<Reservation> findByGuideIdOrderByDateCreationDesc(Long guideId);
}
