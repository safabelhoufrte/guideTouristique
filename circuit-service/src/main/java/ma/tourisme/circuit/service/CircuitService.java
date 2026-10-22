package ma.tourisme.circuit.service;

import java.util.List;
import ma.tourisme.circuit.client.GuideClient;
import ma.tourisme.circuit.dto.CircuitRequest;
import ma.tourisme.circuit.dto.CircuitResponse;
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

  public List<CircuitResponse> list(Long guideId) {
    return repository.findAll(guideId).stream().map(CircuitResponse::from).toList();
  }

  public CircuitResponse get(long id) {
    return CircuitResponse.from(repository.findById(id).orElseThrow(this::notFound));
  }

  public CircuitResponse create(CircuitRequest request) {
    guideClient.requireActive(request.guideId());
    return CircuitResponse.from(
        repository.insert(
            request.titre(), request.prix(), request.guideId(), request.disponible()));
  }

  public CircuitResponse update(long id, CircuitRequest request) {
    get(id);
    guideClient.requireActive(request.guideId());
    return CircuitResponse.from(
        repository
            .update(id, request.titre(), request.prix(), request.guideId(), request.disponible())
            .orElseThrow(this::notFound));
  }

  public void archive(long id) {
    if (!repository.archive(id)) throw notFound();
  }

  public CircuitResponse bookable(long id) {
    CircuitResponse circuit = get(id);
    if (!circuit.disponible()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Circuit indisponible");
    }
    guideClient.requireActive(circuit.guideId());
    return circuit;
  }

  private ResponseStatusException notFound() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Circuit introuvable");
  }
}
