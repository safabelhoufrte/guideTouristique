package ma.tourisme.reservation.repository;

import java.util.List;
import ma.tourisme.reservation.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
  List<Reservation> findByEmail(String email);
}