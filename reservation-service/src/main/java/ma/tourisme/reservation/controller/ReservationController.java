package ma.tourisme.reservation.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ma.tourisme.reservation.dto.ReservationRequest;
import ma.tourisme.reservation.model.Reservation;
import ma.tourisme.reservation.service.ReservationService;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) { this.service = service; }

    @PostMapping
    public Reservation creer(@Valid @RequestBody ReservationRequest req) { return service.creer(req); }

    /** GET /api/reservations ou GET /api/reservations?email=... */
    @GetMapping
    public List<Reservation> lister(@RequestParam(required = false) String email) {
        return service.lister(email);
    }

    @GetMapping("/{id}")
    public Reservation trouver(@PathVariable Long id) { return service.trouver(id); }

    @PatchMapping("/{id}/annuler")
    public Reservation annuler(@PathVariable Long id) { return service.annuler(id); }

    @PatchMapping("/{id}/accepter")
    public Reservation accepter(@PathVariable Long id) { return service.accepter(id); }

    @PatchMapping("/{id}/refuser")
    public Reservation refuser(@PathVariable Long id) { return service.refuser(id); }
}