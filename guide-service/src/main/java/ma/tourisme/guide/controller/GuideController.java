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

import ma.tourisme.guide.model.GuidePhoto;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.CacheControl;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;


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

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> photo(
            @PathVariable Long id) {

        GuidePhoto p = service.photo(id);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(
                p.getContentType()
            ))
            .cacheControl(CacheControl.noCache())
            .eTag(DigestUtils.md5DigestAsHex(p.getData()))
            .body(p.getData());
    }

    @PutMapping(
        value = "/me/photo",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Void> modifierMaPhoto(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file) {

        service.modifierMaPhoto(jwt.getSubject(), file);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me/photo")
    public ResponseEntity<Void> supprimerMaPhoto(
            @AuthenticationPrincipal Jwt jwt) {

        service.supprimerMaPhoto(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
