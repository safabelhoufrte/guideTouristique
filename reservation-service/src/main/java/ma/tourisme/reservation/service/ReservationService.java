package ma.tourisme.reservation.service;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import ma.tourisme.reservation.client.*;
import ma.tourisme.reservation.dto.ReservationRequest;
import ma.tourisme.reservation.model.*;
import ma.tourisme.reservation.repository.ReservationRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReservationService {
  private final ReservationRepository repo;
  private final CircuitClient circuits;
  private final GuideClient guides;
  public ReservationService(ReservationRepository repo,CircuitClient circuits,GuideClient guides){this.repo=repo;this.circuits=circuits;this.guides=guides;}
  private LocalDate today(){return LocalDate.now(ZoneId.of("Africa/Casablanca"));}
  public Reservation creer(ReservationRequest req,Jwt jwt) {
    if(req.dateVisite().isBefore(today()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La date de visite est déjà passée.");
    var circuit=circuits.get(req.circuitId());
    guides.requireAvailable(circuit.guideId());
    var total=circuit.prix().multiply(BigDecimal.valueOf(req.nbPersonnes()));
    if(total.compareTo(new BigDecimal("99999999.99"))>0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Le montant de cette réservation dépasse la limite autorisée.");
    var r=new Reservation();
    r.setUserSubject(jwt.getSubject());r.setCircuitId(circuit.id());r.setGuideId(circuit.guideId());
    r.setDateVisite(req.dateVisite());r.setNbPersonnes(req.nbPersonnes());r.setNom(req.nom().trim());r.setEmail(req.email());
    r.setPrixTotal(total);r.setStatut(StatutReservation.EN_ATTENTE);r.setDateCreation(LocalDateTime.now());
    return repo.save(r);
  }
  public List<Reservation> all(){return repo.findAll(Sort.by(Sort.Direction.DESC,"dateCreation"));}
  public List<Reservation> mine(Jwt jwt){return repo.findByUserSubjectOrderByDateCreationDesc(jwt.getSubject());}
  public List<Reservation> assigned(Jwt jwt){return repo.findByGuideIdOrderByDateCreationDesc(guides.own(jwt));}
  private Reservation find(long id){return repo.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Réservation introuvable"));}
  private boolean admin(Jwt jwt){
    Map<String,Object> realm=jwt.getClaimAsMap("realm_access");
    return realm!=null && realm.get("roles") instanceof Collection<?> roles && roles.contains("ADMIN");
  }
  public Reservation read(long id,Jwt jwt){
    var r=find(id);
    if(!admin(jwt)&&!jwt.getSubject().equals(r.getUserSubject()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Cette réservation ne vous appartient pas.");
    return r;
  }
  @Transactional
  public Reservation decide(long id,StatutReservation status,Jwt jwt){
    var r=read(id,jwt);
    if(r.getStatut()!=StatutReservation.EN_ATTENTE)throw new ResponseStatusException(HttpStatus.CONFLICT,"Seule une demande en attente peut être acceptée ou refusée.");
    if(r.getDateVisite().isBefore(today()))throw new ResponseStatusException(HttpStatus.CONFLICT,"La date de visite est déjà passée.");
    r.setStatut(status);return repo.saveAndFlush(r);
  }
  @Transactional
  public Reservation cancel(long id,Jwt jwt){
    var r=read(id,jwt);
    if(!Set.of(StatutReservation.EN_ATTENTE,StatutReservation.ACCEPTEE).contains(r.getStatut())||r.getDateVisite().isBefore(today()))
      throw new ResponseStatusException(HttpStatus.CONFLICT,"Cette réservation ne peut plus être annulée.");
    r.setStatut(StatutReservation.ANNULEE);return repo.saveAndFlush(r);
  }
  public record Eligibility(long reservationId,long circuitId,long guideId,boolean eligible,String raison){}
  public Eligibility eligibility(long id,Jwt jwt){
    var r=read(id,jwt);
    boolean allowed=r.getStatut()==StatutReservation.ACCEPTEE&&r.getDateVisite().isBefore(today());
    return new Eligibility(r.getId(),r.getCircuitId(),r.getGuideId(),allowed,
      allowed?"Vous pouvez publier votre avis.":"Un avis est possible après la date d’une visite acceptée.");
  }
}
