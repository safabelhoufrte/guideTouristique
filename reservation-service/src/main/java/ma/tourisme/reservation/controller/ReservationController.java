package ma.tourisme.reservation.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ma.tourisme.reservation.model.Reservation;
import ma.tourisme.reservation.service.ReservationService;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
    private final ReservationService service;
    public ReservationController(ReservationService service) { this.service = service; }

    @PostMapping
    public Reservation creer(@RequestBody Reservation r) { return service.creer(r); }

    @GetMapping
    public List<Reservation> lister() { return service.lister(); }

    @GetMapping("/{id}")
    public Reservation trouver(@PathVariable Long id) { return service.trouver(id); }
}