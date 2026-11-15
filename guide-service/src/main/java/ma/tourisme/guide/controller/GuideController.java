package ma.tourisme.guide.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import ma.tourisme.guide.dto.GuideRequest;
import ma.tourisme.guide.dto.GuideResponse;
import ma.tourisme.guide.service.GuideService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import ma.tourisme.guide.dto.GuideAdminResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@Validated
@RestController
@RequestMapping("/api/guides")
public class GuideController {
    private final GuideService service;

    public GuideController(GuideService service) { this.service = service; }

    @GetMapping
    public List<GuideResponse> getAll(
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) String langue) {
        return service.findAll(ville, langue);
    }

    @GetMapping("/{id}")
    public GuideResponse getById(@PathVariable @Positive Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GuideResponse create(@Valid @RequestBody GuideRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public GuideResponse update(@PathVariable @Positive Long id,
                                @Valid @RequestBody GuideRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive Long id) {
        service.desactiver(id);
    }


    /** ADMIN: every guide, including inactive ones and the linked Keycloak account. */
    @GetMapping("/admin")
    public List<GuideAdminResponse> admin() {
        return service.findAllForAdmin();
    }

    /** GUIDE: the profile linked to the caller's Keycloak account (never an id sent by the client). */
    @GetMapping("/me")
    public GuideResponse monProfil(@AuthenticationPrincipal Jwt jwt) {
        return service.findByOwner(jwt.getSubject());
    }

    @PutMapping("/me")
    public GuideResponse modifierMonProfil(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody GuideRequest request) {
        return service.updateMine(jwt.getSubject(), request);
    }
}
