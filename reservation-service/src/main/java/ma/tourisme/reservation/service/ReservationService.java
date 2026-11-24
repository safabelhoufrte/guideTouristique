package ma.tourisme.reservation.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ma.tourisme.reservation.client.CircuitClient;
import ma.tourisme.reservation.client.GuideClient;
import ma.tourisme.reservation.dto.ReservationRequest;
import ma.tourisme.reservation.model.Reservation;
import ma.tourisme.reservation.repository.ReservationRepository;

@Service
public class ReservationService {

    public static final String EN_ATTENTE = "EN_ATTENTE";
    public static final String ACCEPTEE = "ACCEPTEE";
    public static final String REFUSEE = "REFUSEE";
    public static final String ANNULEE = "ANNULEE";

    private final ReservationRepository repository;
    private final CircuitClient circuitClient;
    private final GuideClient guideClient;
    private final PromoService promoService;

    public ReservationService(ReservationRepository repository,
                              CircuitClient circuitClient,
                              GuideClient guideClient,
                              PromoService promoService) {
        this.repository = repository;
        this.circuitClient = circuitClient;
        this.guideClient = guideClient;
        this.promoService = promoService;
    }

    @Transactional
    public Reservation creer(ReservationRequest req) {
        var circuit = circuitClient.get(req.circuitId());
        guideClient.requireAvailable(circuit.guideId());

        String email = req.email().trim().toLowerCase();
        boolean dejaReserve = repository
            .findByCircuitIdAndDateVisite(req.circuitId(), req.dateVisite())
            .stream()
            .anyMatch(x -> email.equalsIgnoreCase(x.getEmail())
                && (EN_ATTENTE.equals(x.getStatut()) || ACCEPTEE.equals(x.getStatut())));
        if (dejaReserve) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Vous avez déjà réservé ce circuit à cette date");
        }

        Reservation r = new Reservation();
        r.setCircuitId(circuit.id());
        r.setGuideId(circuit.guideId());
        r.setDateVisite(req.dateVisite());
        r.setNbPersonnes(req.nbPersonnes());
        r.setNom(req.nom().trim());
        r.setEmail(email);
        BigDecimal sousTotal = circuit.prix().multiply(BigDecimal.valueOf(req.nbPersonnes()));
        int pct = 0;
        if (req.codePromo() != null && !req.codePromo().isBlank()) {
            pct = promoService.appliquer(req.codePromo(), email, req.nbPersonnes());
            r.setCodePromo(req.codePromo().trim().toUpperCase());
        }
        r.setReductionPct(pct);
        r.setPrixTotal(PromoService.prixApresReduction(sousTotal, pct));
        r.setStatut(EN_ATTENTE);
        return repository.save(r);
    }

    public List<Reservation> lister(String email) {
        if (email == null || email.isBlank()) return repository.findAll();
        return repository.findByEmail(email.trim().toLowerCase());
    }

    public Reservation trouver(Long id) {
        return repository.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Réservation introuvable"));
    }

    @Transactional
    public Reservation annuler(Long id) {
        Reservation r = trouver(id);
        if (!EN_ATTENTE.equals(r.getStatut()) && !ACCEPTEE.equals(r.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette réservation ne peut plus être annulée");
        }
        r.setStatut(ANNULEE);
        return repository.save(r);
    }

    @Transactional
    public Reservation accepter(Long id) {
        return decider(id, ACCEPTEE);
    }

    @Transactional
    public Reservation refuser(Long id) {
        return decider(id, REFUSEE);
    }

    private Reservation decider(Long id, String nouveauStatut) {
        Reservation r = trouver(id);
        if (!EN_ATTENTE.equals(r.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seule une réservation en attente peut être traitée");
        }
        r.setStatut(nouveauStatut);
        return repository.save(r);
    }


    @Transactional(readOnly = true)
    public List<Reservation> reservationsDuGuide(
            String accessToken) {

        Long guideId =
            guideClient.getAuthenticatedGuideId(accessToken);

        return repository
            .findByGuideIdOrderByDateCreationDesc(guideId);
    }

    @Transactional
    public Reservation accepterPourGuide(
            Long reservationId,
            String accessToken) {

        return deciderPourGuide(
            reservationId,
            accessToken,
            ACCEPTEE
        );
    }

    @Transactional
    public Reservation refuserPourGuide(
            Long reservationId,
            String accessToken) {

        return deciderPourGuide(
            reservationId,
            accessToken,
            REFUSEE
        );
    }

    private Reservation deciderPourGuide(
            Long reservationId,
            String accessToken,
            String nouveauStatut) {

        // Identifier le guide connecté via Keycloak
        Long guideId =
            guideClient.getAuthenticatedGuideId(accessToken);

        // Chercher la réservation
        Reservation reservation = trouver(reservationId);

        // Empêcher un guide de traiter les demandes d'un autre
        if (!guideId.equals(reservation.getGuideId())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Cette reservation appartient a un autre guide"
            );
        }

        // Réutiliser la logique métier existante de Mariam
        return decider(reservationId, nouveauStatut);
    }

}