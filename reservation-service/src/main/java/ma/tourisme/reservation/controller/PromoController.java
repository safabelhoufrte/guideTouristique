package ma.tourisme.reservation.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ma.tourisme.reservation.dto.PromoResultat;
import ma.tourisme.reservation.service.PromoService;

@RestController
@RequestMapping("/api/reservations/promo")
public class PromoController {

    private final PromoService service;

    public PromoController(PromoService service) { this.service = service; }

    /** GET /api/reservations/promo/BIENVENUE?email=...&nbPersonnes=2 */
    @GetMapping("/{code}")
    public PromoResultat verifier(@PathVariable String code,
                                  @RequestParam(required = false) String email,
                                  @RequestParam(defaultValue = "1") int nbPersonnes) {
        return service.verifier(code, email, nbPersonnes);
    }
}