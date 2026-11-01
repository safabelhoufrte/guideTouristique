package ma.tourisme.reservation.controller;
import jakarta.validation.Valid;
import java.util.List;
import ma.tourisme.reservation.dto.ReservationRequest;
import ma.tourisme.reservation.model.*;
import ma.tourisme.reservation.service.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
  private final ReservationService service;
  public ReservationController(ReservationService service){this.service=service;}
  @PostMapping @ResponseStatus(HttpStatus.CREATED)
  public Reservation create(@Valid @RequestBody ReservationRequest req,@AuthenticationPrincipal Jwt jwt){return service.creer(req,jwt);}
  @GetMapping public List<Reservation> all(){return service.all();}
  @GetMapping("/me") public List<Reservation> mine(@AuthenticationPrincipal Jwt jwt){return service.mine(jwt);}
  @GetMapping("/guide") public List<Reservation> assigned(@AuthenticationPrincipal Jwt jwt){return service.assigned(jwt);}
  @GetMapping("/{id}") public Reservation get(@PathVariable long id,@AuthenticationPrincipal Jwt jwt){return service.read(id,jwt);}
  @GetMapping("/{id}/eligibilite") public ReservationService.Eligibility eligibility(@PathVariable long id,@AuthenticationPrincipal Jwt jwt){return service.eligibility(id,jwt);}
  @PatchMapping("/{id}/annuler") public Reservation cancel(@PathVariable long id,@AuthenticationPrincipal Jwt jwt){return service.cancel(id,jwt);}
  @PatchMapping("/{id}/accepter") public Reservation accept(@PathVariable long id,@AuthenticationPrincipal Jwt jwt){return service.decide(id,StatutReservation.ACCEPTEE,jwt);}
  @PatchMapping("/{id}/refuser") public Reservation refuse(@PathVariable long id,@AuthenticationPrincipal Jwt jwt){return service.decide(id,StatutReservation.REFUSEE,jwt);}
}
