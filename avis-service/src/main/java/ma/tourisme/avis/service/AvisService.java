package ma.tourisme.avis.service;
import java.util.*;
import ma.tourisme.avis.client.ReservationClient;
import ma.tourisme.avis.dto.*;
import ma.tourisme.avis.repository.AvisRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
@Service
public class AvisService {
  private final AvisRepository repo;
  private final ReservationClient reservations;
  public AvisService(AvisRepository repo,ReservationClient reservations){this.repo=repo;this.reservations=reservations;}
  public List<AvisResponse> list(Long circuitId,Long guideId,boolean admin){return repo.list(circuitId,guideId,admin);}
  public List<AvisResponse> mine(Jwt jwt){return repo.mine(jwt.getSubject());}
  public AvisResponse create(AvisRequest r,Jwt jwt){
    var refs=reservations.requireEligible(r.reservationId(),jwt);
    var name=jwt.getClaimAsString("name");
    if(name==null||name.isBlank())name="Voyageur";
    if(name.length()>160)name=name.substring(0,160);
    try{return repo.insert(r,refs,jwt.getSubject(),name);}
    catch(DuplicateKeyException ex){throw new ResponseStatusException(HttpStatus.CONFLICT,"Un avis existe déjà pour cette réservation.");}
  }
  public AvisResponse publication(long id,boolean publie){return repo.publication(id,publie).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Avis introuvable"));}
}
