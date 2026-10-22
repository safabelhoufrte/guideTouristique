package ma.tourisme.circuit.controller;

import jakarta.validation.Valid;
import java.util.List;
import ma.tourisme.circuit.dto.CircuitRequest;
import ma.tourisme.circuit.dto.CircuitResponse;
import ma.tourisme.circuit.service.CircuitService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class CircuitController {
  private final CircuitService service;

  public CircuitController(CircuitService service) {
    this.service = service;
  }

  @GetMapping("/circuits")
  public List<CircuitResponse> list(
      @RequestParam(name = "guide_id", required = false) Long guideId) {
    return service.list(guideId);
  }

  @GetMapping("/guides/{guideId}/circuits")
  public List<CircuitResponse> byGuide(@PathVariable long guideId) {
    return service.list(guideId);
  }

  @GetMapping("/circuits/{id}")
  public CircuitResponse get(@PathVariable long id) {
    return service.get(id);
  }

  @PostMapping("/circuits")
  @ResponseStatus(HttpStatus.CREATED)
  public CircuitResponse create(@Valid @RequestBody CircuitRequest request) {
    return service.create(request);
  }

  @PutMapping("/circuits/{id}")
  public CircuitResponse update(@PathVariable long id, @Valid @RequestBody CircuitRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/circuits/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void archive(@PathVariable long id) {
    service.archive(id);
  }

  @GetMapping("/circuits/{id}/reservation-details")
  public CircuitResponse bookable(@PathVariable long id) {
    return service.bookable(id);
  }
}
