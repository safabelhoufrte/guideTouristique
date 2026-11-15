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
import org.springframework.security.core.Authentication;

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


    @GetMapping("/me")
    public GuideResponse monProfil(Authentication authentication) {
        Long guideId = getConnectedGuideId(authentication);
        return service.findById(guideId);
    }

    @PutMapping("/me")
    public GuideResponse modifierMonProfil(
            Authentication authentication,
            @Valid @RequestBody GuideRequest request) {

        Long guideId = getConnectedGuideId(authentication);
        return service.update(guideId, request);
    }

    private Long getConnectedGuideId(Authentication authentication) {
        if (authentication == null
                || !authentication.getName().startsWith("guide:")) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED,
                "Authentification requise"
            );
        }

        return Long.valueOf(
            authentication.getName().substring(6)
        );
    }
}
