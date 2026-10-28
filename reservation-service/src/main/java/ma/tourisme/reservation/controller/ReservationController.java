package ma.tourisme.reservation.controller;

import jakarta.validation.Valid;
import java.util.List;
import ma.tourisme.reservation.dto.ReservationRequest;
import ma.tourisme.reservation.model.Reservation;
import ma.tourisme.reservation.model.StatutReservation;
import ma.tourisme.reservation.service.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
  private final ReservationService service;

  public ReservationController(ReservationService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Reservation creer(@Valid @RequestBody ReservationRequest request) {
    return service.creer(request);
  }

  @GetMapping
  public List<Reservation> lister(@RequestParam(required = false) String email) {
    return service.lister(email);
  }

  @GetMapping("/{id}")
  public Reservation get(@PathVariable long id) {
    return service.trouver(id);
  }

  @PatchMapping("/{id}/annuler")
  public Reservation annuler(@PathVariable long id) {
    return service.changerStatut(id, StatutReservation.ANNULEE);
  }

  @PatchMapping("/{id}/accepter")
  public Reservation accepter(@PathVariable long id) {
    return service.changerStatut(id, StatutReservation.ACCEPTEE);
  }

  @PatchMapping("/{id}/refuser")
  public Reservation refuser(@PathVariable long id) {
    return service.changerStatut(id, StatutReservation.REFUSEE);
  }
}