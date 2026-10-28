package ma.tourisme.reservation.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import ma.tourisme.reservation.client.CircuitClient;
import ma.tourisme.reservation.client.GuideClient;
import ma.tourisme.reservation.dto.ReservationRequest;
import ma.tourisme.reservation.model.Reservation;
import ma.tourisme.reservation.model.StatutReservation;
import ma.tourisme.reservation.repository.ReservationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReservationService {
  private final ReservationRepository repo;
  private final CircuitClient circuits;
  private final GuideClient guides;

  public ReservationService(ReservationRepository repo, CircuitClient circuits, GuideClient guides) {
    this.repo = repo;
    this.circuits = circuits;
    this.guides = guides;
  }

  public Reservation creer(ReservationRequest req) {
    var circuit = circuits.get(req.circuitId());
    guides.requireAvailable(circuit.guideId());

    Reservation r = new Reservation();
    r.setCircuitId(circuit.id());
    r.setGuideId(circuit.guideId());
    r.setDateVisite(req.dateVisite());
    r.setNbPersonnes(req.nbPersonnes());
    r.setNom(req.nom());
    r.setEmail(req.email());
    r.setPrixTotal(circuit.prix().multiply(BigDecimal.valueOf(req.nbPersonnes())));
    r.setStatut(StatutReservation.EN_ATTENTE);
    r.setDateCreation(LocalDateTime.now());
    return repo.save(r);
  }

  public List<Reservation> lister(String email) {
    return email == null || email.isBlank() ? repo.findAll() : repo.findByEmail(email);
  }

  public Reservation trouver(long id) {
    return repo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réservation introuvable"));
  }

  public Reservation changerStatut(long id, StatutReservation statut) {
    Reservation r = trouver(id);
    if (r.getStatut() == StatutReservation.ANNULEE) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Réservation déjà annulée");
    }
    r.setStatut(statut);
    return repo.save(r);
  }
}