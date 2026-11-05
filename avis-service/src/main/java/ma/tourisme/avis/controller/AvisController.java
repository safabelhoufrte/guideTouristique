package ma.tourisme.avis.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import ma.tourisme.avis.dto.AvisRequest;
import ma.tourisme.avis.dto.AvisResponse;
import ma.tourisme.avis.dto.MoyenneResponse;
import ma.tourisme.avis.service.AvisService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/avis")
public class AvisController {
  private final AvisService service;

  public AvisController(AvisService service) {
    this.service = service;
  }

  @GetMapping
  public List<AvisResponse> list(
      @RequestParam(required = false) Long circuitId,
      @RequestParam(required = false) Long guideId,
      @RequestParam(required = false) String cibleType,
      @RequestParam(required = false) Long cibleId) {
    Long targetCircuit = circuitId;
    Long targetGuide = guideId;
    if (cibleType != null && cibleId != null) {
      if ("CIRCUIT".equalsIgnoreCase(cibleType)) {
        targetCircuit = cibleId;
      } else if ("GUIDE".equalsIgnoreCase(cibleType)) {
        targetGuide = cibleId;
      }
    }
    return service.list(targetCircuit, targetGuide, false);
  }

  @GetMapping("/admin")
  public List<AvisResponse> all() {
    return service.list(null, null, true);
  }

  @GetMapping("/me")
  public List<AvisResponse> mine(@AuthenticationPrincipal Jwt jwt) {
    return service.mine(jwt);
  }

  @GetMapping("/moyenne")
  public MoyenneResponse moyenne(
      @RequestParam(required = false) Long circuitId,
      @RequestParam(required = false) Long guideId,
      @RequestParam(required = false) String cibleType,
      @RequestParam(required = false) Long cibleId) {
    return service.moyenne(circuitId, guideId, cibleType, cibleId);
  }

  @GetMapping("/{id}")
  public AvisResponse getById(@PathVariable long id, Authentication auth) {
    boolean isAdmin = auth != null && auth.getAuthorities().stream()
        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    return service.getById(id, isAdmin);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AvisResponse create(@Valid @RequestBody AvisRequest request, @AuthenticationPrincipal Jwt jwt) {
    return service.create(request, jwt);
  }

  public record Publication(@NotNull(message = "Le statut de publication est obligatoire") Boolean publie) {}

  @PatchMapping("/{id}/publication")
  public AvisResponse publication(@PathVariable long id, @Valid @RequestBody Publication request) {
    return service.publication(id, request.publie());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable long id) {
    service.delete(id);
  }
}
