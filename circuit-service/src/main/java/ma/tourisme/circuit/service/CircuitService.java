package ma.tourisme.circuit.service;

import java.util.List;
import ma.tourisme.circuit.client.GuideClient;
import ma.tourisme.circuit.dto.CircuitRequest;
import ma.tourisme.circuit.dto.CircuitResponse;
import ma.tourisme.circuit.model.Circuit;
import ma.tourisme.circuit.repository.CircuitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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
        request.guideId());
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

  public void archive(long id) {
    if (!repository.archive(id)) throw notFound();
  }

  private ResponseStatusException notFound() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Circuit introuvable");
  }
}
