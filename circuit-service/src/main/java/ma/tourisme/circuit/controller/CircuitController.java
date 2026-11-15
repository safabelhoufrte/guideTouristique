package ma.tourisme.circuit.controller;

import jakarta.validation.Valid;
import java.util.List;
import ma.tourisme.circuit.dto.CircuitRequest;
import ma.tourisme.circuit.dto.CircuitResponse;
import ma.tourisme.circuit.service.CircuitService;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/circuits")
public class CircuitController {
  private final CircuitService service;

  public CircuitController(CircuitService service) {
    this.service = service;
  }

  @GetMapping
  public List<CircuitResponse> list(
      @RequestParam(required = false) String ville, @RequestParam(required = false) Long guideId) {
    return service.list(ville, guideId);
  }

  @GetMapping("/{id}")
  public CircuitResponse get(@PathVariable long id) {
    return service.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CircuitResponse create(
      @Valid @RequestBody CircuitRequest request, JwtAuthenticationToken caller) {
    return isAdmin(caller)
        ? service.create(request)
        : service.createAsGuide(request, caller.getToken().getTokenValue());
  }

  @PutMapping("/{id}")
  public CircuitResponse update(
      @PathVariable long id,
      @Valid @RequestBody CircuitRequest request,
      JwtAuthenticationToken caller) {
    return isAdmin(caller)
        ? service.update(id, request)
        : service.updateAsGuide(id, request, caller.getToken().getTokenValue());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void archive(@PathVariable long id, JwtAuthenticationToken caller) {
    if (isAdmin(caller)) service.archive(id);
    else service.archiveAsGuide(id, caller.getToken().getTokenValue());
  }

  /** SecurityConfig only lets ADMIN or GUIDE reach writes; anything not ADMIN is a guide. */
  private static boolean isAdmin(JwtAuthenticationToken caller) {
    return caller.getAuthorities().stream()
        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
  }
}
