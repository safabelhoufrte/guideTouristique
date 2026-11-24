package ma.tourisme.circuit.controller;

import jakarta.validation.Valid;
import java.util.List;
import ma.tourisme.circuit.dto.CircuitRequest;
import ma.tourisme.circuit.dto.CircuitResponse;
import ma.tourisme.circuit.service.CircuitService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST API of the Circuit service: public catalogue reads, ADMIN and GUIDE writes. */
@RestController
@RequestMapping("/api/circuits")
public class CircuitController {
  private final CircuitService service;

  /** Creates the controller on top of the business service. */
  public CircuitController(CircuitService service) {
    this.service = service;
  }

  /** Available circuits, optionally filtered by city and/or guide. */
  @GetMapping
  public List<CircuitResponse> list(
      @RequestParam(required = false) String ville, @RequestParam(required = false) Long guideId) {
    return service.list(ville, guideId);
  }

  /** One available circuit, 404 if it does not exist or is archived. */
  @GetMapping("/{id}")
  public CircuitResponse get(@PathVariable long id) {
    return service.get(id);
  }

  /** Public photo of a circuit. The ETag lets browsers revalidate instead of downloading again. */
  @GetMapping("/{id}/image")
  public ResponseEntity<byte[]> image(@PathVariable long id) {
    CircuitService.Photo photo = service.photo(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(photo.contentType()))
        .cacheControl(CacheControl.noCache())
        .eTag(photo.etag())
        .body(photo.bytes());
  }

  /** Creates a circuit: any guide for ADMIN, only the caller's own guide for GUIDE. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CircuitResponse create(
      @Valid @RequestBody CircuitRequest request, JwtAuthenticationToken caller) {
    return isAdmin(caller)
        ? service.create(request)
        : service.createAsGuide(request, caller.getToken().getTokenValue());
  }

  /** Updates a circuit; a GUIDE can only update their own circuits. */
  @PutMapping("/{id}")
  public CircuitResponse update(
      @PathVariable long id,
      @Valid @RequestBody CircuitRequest request,
      JwtAuthenticationToken caller) {
    return isAdmin(caller)
        ? service.update(id, request)
        : service.updateAsGuide(id, request, caller.getToken().getTokenValue());
  }

  /** Archives a circuit (kept for history); a GUIDE can only archive their own. */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void archive(@PathVariable long id, JwtAuthenticationToken caller) {
    if (isAdmin(caller)) {
      service.archive(id);
    } else {
      service.archiveAsGuide(id, caller.getToken().getTokenValue());
    }
  }

  /** SecurityConfig only lets ADMIN or GUIDE reach writes; anything not ADMIN is a guide. */
  private static boolean isAdmin(JwtAuthenticationToken caller) {
    return caller.getAuthorities().stream()
        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
  }
}
