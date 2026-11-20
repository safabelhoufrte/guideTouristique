package ma.tourisme.circuit.service;

import java.util.Base64;
import java.util.List;
import ma.tourisme.circuit.client.GuideClient;
import ma.tourisme.circuit.dto.CircuitRequest;
import ma.tourisme.circuit.dto.CircuitResponse;
import ma.tourisme.circuit.model.Circuit;
import ma.tourisme.circuit.repository.CircuitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CircuitService {
  private final CircuitRepository repository;
  private final GuideClient guideClient;

  public CircuitService(CircuitRepository repository, GuideClient guideClient) {
    this.repository = repository;
    this.guideClient = guideClient;
  }

  public List<CircuitResponse> list(String ville, Long guideId) {
    return repository.findAll(ville, guideId).stream().map(CircuitResponse::from).toList();
  }

  public CircuitResponse get(long id) {
    return CircuitResponse.from(repository.findById(id).orElseThrow(this::notFound));
  }

  private Circuit model(long id, CircuitRequest request) {
    return new Circuit(
        id,
        request.titre(),
        request.description(),
        request.ville(),
        request.dureeHeures(),
        request.prix(),
        request.guideId(),
        request.image());
  }

  public CircuitResponse create(CircuitRequest request) {
    guideClient.requireExisting(request.guideId());
    return CircuitResponse.from(repository.insert(model(0, request)));
  }

  public CircuitResponse update(long id, CircuitRequest request) {
    get(id);
    guideClient.requireExisting(request.guideId());
    return CircuitResponse.from(repository.update(model(id, request)).orElseThrow(this::notFound));
  }

  /** Photo of a circuit as bytes, decoded from the stored data URL. */
  public record Photo(String contentType, byte[] bytes, String etag) {}

  public Photo photo(long id) {
    String dataUrl =
        repository
            .findImage(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo introuvable"));
    int comma = dataUrl.indexOf(',');
    String contentType = dataUrl.substring("data:".length(), dataUrl.indexOf(';'));
    byte[] bytes = Base64.getDecoder().decode(dataUrl.substring(comma + 1));
    return new Photo(contentType, bytes, DigestUtils.md5DigestAsHex(bytes));
  }

  public void archive(long id) {
    if (!repository.archive(id)) throw notFound();
  }

  // ---- GUIDE role: same operations, limited to the caller's own circuits ----

  public CircuitResponse createAsGuide(CircuitRequest request, String bearerToken) {
    requireOwnership(guideClient.currentGuideId(bearerToken), null, request.guideId());
    return create(request);
  }

  public CircuitResponse updateAsGuide(long id, CircuitRequest request, String bearerToken) {
    Circuit existing = repository.findById(id).orElseThrow(this::notFound);
    requireOwnership(
        guideClient.currentGuideId(bearerToken), existing.guideId(), request.guideId());
    return update(id, request);
  }

  public void archiveAsGuide(long id, String bearerToken) {
    Circuit existing = repository.findById(id).orElseThrow(this::notFound);
    requireOwnership(
        guideClient.currentGuideId(bearerToken), existing.guideId(), existing.guideId());
    archive(id);
  }

  /**
   * Ownership rule for a GUIDE write.
   *
   * @param ownGuideId guideId of the logged-in guide (resolved by the Guide service)
   * @param currentOwner guideId stored on the circuit today, or null when creating
   * @param requestedOwner guideId the circuit will have after the write
   * @throws ResponseStatusException 403 when the guide may not perform this write
   */
  static void requireOwnership(long ownGuideId, Long currentOwner, long requestedOwner) {
    // A guide never creates or assigns a circuit for another guide.
    if (requestedOwner != ownGuideId) throw forbidden();
    // A circuit already linked to another guide cannot be taken over.
    if (currentOwner != null && currentOwner != ownGuideId) throw forbidden();
  }

  private static ResponseStatusException forbidden() {
    return new ResponseStatusException(
        HttpStatus.FORBIDDEN, "Ce circuit n'appartient pas au guide connecté");
  }

  private ResponseStatusException notFound() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Circuit introuvable");
  }
}
