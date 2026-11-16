package ma.tourisme.reservation.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ma.tourisme.reservation.model.Reservation;
import ma.tourisme.reservation.repository.ReservationRepository;
@Service
public class ReservationService {
    private final ReservationRepository repository;
    public ReservationService(ReservationRepository repository) { this.repository = repository; }

    @Transactional
    public Reservation creer(Reservation r) { return repository.save(r); }

    public List<Reservation> lister() { return repository.findAll(); }

    public Reservation trouver(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réservation introuvable"));
    }
}