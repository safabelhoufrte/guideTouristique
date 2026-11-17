package ma.tourisme.reservation.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ma.tourisme.reservation.dto.PromoResultat;
import ma.tourisme.reservation.model.CodePromo;
import ma.tourisme.reservation.repository.CodePromoRepository;
import ma.tourisme.reservation.repository.ReservationRepository;

@Service
public class PromoService {

    private final CodePromoRepository codes;
    private final ReservationRepository reservations;

    public PromoService(CodePromoRepository codes, ReservationRepository reservations) {
        this.codes = codes;
        this.reservations = reservations;
    }

    /** Verifie un code sans le consommer. */
    public PromoResultat verifier(String code, String email, int nbPersonnes) {
        String c = code == null ? "" : code.trim().toUpperCase();
        CodePromo p = c.isEmpty() ? null : codes.findById(c).orElse(null);
        if (p == null || !Boolean.TRUE.equals(p.getActif())) {
            return refus(c, "Code promo invalide.");
        }
        if (p.getDateExpiration() != null && p.getDateExpiration().isBefore(LocalDate.now())) {
            return refus(c, "Ce code a expiré.");
        }
        if (p.getUtilisationsMax() != null && p.getUtilisations() >= p.getUtilisationsMax()) {
            return refus(c, "Ce code n'est plus disponible.");
        }
        if (nbPersonnes < p.getPersonnesMin()) {
            return refus(c, "Ce code est valable à partir de " + p.getPersonnesMin() + " personnes.");
        }
        if (Boolean.TRUE.equals(p.getPremiereReservationSeulement()) && email != null && !email.isBlank()) {
            boolean dejaReserve = reservations.findByEmail(email.trim().toLowerCase()).stream()
                .anyMatch(r -> ReservationService.EN_ATTENTE.equals(r.getStatut())
                    || ReservationService.ACCEPTEE.equals(r.getStatut()));
            if (dejaReserve) {
                return refus(c, "Ce code est réservé à votre première réservation.");
            }
        }
        return new PromoResultat(true, c, p.getPourcentage(), "Code appliqué : -" + p.getPourcentage() + " %");
    }

    /** Verifie puis consomme le code (+1 utilisation). Renvoie le pourcentage. */
    @Transactional
    public int appliquer(String code, String email, int nbPersonnes) {
        PromoResultat r = verifier(code, email, nbPersonnes);
        if (!r.valide()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, r.message());
        }
        CodePromo p = codes.findById(r.code()).orElseThrow();
        p.setUtilisations(p.getUtilisations() + 1);
        codes.save(p);
        return r.pourcentage();
    }

    /** Total x (100 - pct) / 100, arrondi a l'entier (comme l'affichage Angular). */
    public static BigDecimal prixApresReduction(BigDecimal sousTotal, int pct) {
        if (pct <= 0) return sousTotal;
        return sousTotal.multiply(BigDecimal.valueOf(100 - pct))
            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
            .setScale(2);
    }

    private PromoResultat refus(String code, String message) {
        return new PromoResultat(false, code, 0, message);
    }
}