package ma.tourisme.avis.service;

import java.util.List;
import ma.tourisme.avis.client.ReservationClient;
import ma.tourisme.avis.dto.AvisRequest;
import ma.tourisme.avis.dto.AvisResponse;
import ma.tourisme.avis.dto.MoyenneResponse;
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

  public AvisService(AvisRepository repo, ReservationClient reservations) {
    this.repo = repo;
    this.reservations = reservations;
  }

  public List<AvisResponse> list(Long circuitId, Long guideId, boolean admin) {
    return repo.list(circuitId, guideId, admin);
  }

  public AvisResponse getById(long id, boolean isAdmin) {
    AvisResponse avis = repo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avis introuvable"));
    if (!avis.publie() && !isAdmin) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Avis introuvable");
    }
    return avis;
  }

  public List<AvisResponse> mine(Jwt jwt) {
    return repo.mine(jwt.getSubject());
  }

  public AvisResponse create(AvisRequest r, Jwt jwt) {
    var refs = reservations.requireEligible(r.reservationId(), jwt);
    var name = jwt.getClaimAsString("name");
    if (name == null || name.isBlank()) {
      name = "Voyageur";
    }
    if (name.length() > 160) {
      name = name.substring(0, 160);
    }
    try {
      return repo.insert(r, refs, jwt.getSubject(), name);
    } catch (DuplicateKeyException ex) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Un avis existe déjà pour cette réservation."
      );
    }
  }

  public AvisResponse publication(long id, boolean publie) {
    return repo.publication(id, publie)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avis introuvable"));
  }

  public void delete(long id) {
    if (!repo.delete(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Avis introuvable");
    }
  }

  public MoyenneResponse moyenne(Long circuitId, Long guideId, String cibleType, Long cibleId) {
    Long targetCircuit = circuitId;
    Long targetGuide = guideId;

    if (cibleType != null && cibleId != null) {
      if ("CIRCUIT".equalsIgnoreCase(cibleType)) {
        targetCircuit = cibleId;
      } else if ("GUIDE".equalsIgnoreCase(cibleType)) {
        targetGuide = cibleId;
      }
    }

    if (targetCircuit != null) {
      return repo.calculerMoyenneCircuit(targetCircuit);
    }
    if (targetGuide != null) {
      return repo.calculerMoyenneGuide(targetGuide);
    }
    throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Précisez circuitId ou guideId (ou cibleType et cibleId)."
    );
  }
}
