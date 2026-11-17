package ma.tourisme.reservation.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ma.tourisme.reservation.dto.PromoResultat;
import ma.tourisme.reservation.model.CodePromo;
import ma.tourisme.reservation.repository.CodePromoRepository;
import ma.tourisme.reservation.service.PromoService;

@RestController
@RequestMapping("/api/reservations/offres")
public class OffreController {

    private final CodePromoRepository codes;
    private final PromoService promoService;

    public OffreController(CodePromoRepository codes, PromoService promoService) {
        this.codes = codes;
        this.promoService = promoService;
    }

    /** Code de bienvenue a afficher a un touriste (valide = false s'il n'y a rien a lui proposer). */
    @GetMapping("/bienvenue")
    public PromoResultat bienvenue(@RequestParam(required = false) String email) {
        if (email == null || email.isBlank()) return new PromoResultat(false, "", 0, "");
        for (CodePromo p : codes.findByBienvenueTrueAndActifTrue()) {
            PromoResultat r = promoService.verifier(p.getCode(), email, p.getPersonnesMin());
            if (r.valide()) return r;
        }
        return new PromoResultat(false, "", 0, "");
    }
}