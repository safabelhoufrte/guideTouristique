
package ma.tourisme.guide.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import ma.tourisme.guide.dto.GuideAdminResponse;
import ma.tourisme.guide.dto.GuideRequest;
import ma.tourisme.guide.dto.GuideResponse;
import ma.tourisme.guide.model.GuidePhoto;
import ma.tourisme.guide.service.GuideService;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.DigestUtils;
import org.springframework.validation.annotation.Validated;
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
import org.springframework.web.multipart.MultipartFile;

/**
 * Contrôleur REST responsable des opérations sur les guides touristiques.
 *
 * <p>Il expose les API publiques, les opérations administrateur,
 * la gestion du profil personnel et les photos des guides.
 */
@Validated
@RestController
@RequestMapping("/api/guides")
public class GuideController {

    /** Service métier utilisé par le contrôleur. */
    private final GuideService service;

    /**
     * Initialise le contrôleur avec son service métier.
     *
     * @param guideService service de gestion des guides
     */
    public GuideController(final GuideService guideService) {
        this.service = guideService;
    }

    /**
     * Retourne la liste des guides avec des filtres facultatifs.
     *
     * @param ville filtre par ville
     * @param langue filtre par langue
     * @return liste des guides correspondants
     */
    @GetMapping
    public List<GuideResponse> getAll(
            @RequestParam(required = false) final String ville,
            @RequestParam(required = false) final String langue) {

        return service.findAll(ville, langue);
    }

    /**
     * Recherche un guide par son identifiant.
     *
     * @param id identifiant du guide
     * @return informations du guide trouvé
     */
    @GetMapping("/{id}")
    public GuideResponse getById(
            @PathVariable @Positive final Long id) {

        return service.findById(id);
    }

    /**
     * Crée un nouveau guide.
     *
     * <p>Cette opération est réservée à l'administrateur.
     *
     * @param request informations du guide à créer
     * @return informations du guide créé
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GuideResponse create(
            @Valid @RequestBody final GuideRequest request) {

        return service.create(request);
    }

    /**
     * Modifie les informations d'un guide.
     *
     * <p>Cette opération est réservée à l'administrateur.
     *
     * @param id identifiant du guide
     * @param request nouvelles informations
     * @return informations du guide mis à jour
     */
    @PutMapping("/{id}")
    public GuideResponse update(
            @PathVariable @Positive final Long id,
            @Valid @RequestBody final GuideRequest request) {

        return service.update(id, request);
    }

    /**
     * Désactive un guide sans supprimer son enregistrement.
     *
     * <p>Cette opération est réservée à l'administrateur.
     *
     * @param id identifiant du guide
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable @Positive final Long id) {

        service.desactiver(id);
    }

    /**
     * Retourne tous les guides, y compris les guides inactifs.
     *
     * <p>Cette opération est réservée à l'administrateur.
     *
     * @return liste complète des guides
     */
    @GetMapping("/admin")
    public List<GuideAdminResponse> admin() {

        return service.findAllForAdmin();
    }

    /**
     * Retourne le profil du guide authentifié.
     *
     * <p>Le guide est identifié à partir du sujet Keycloak.
     *
     * @param jwt token du guide connecté
     * @return profil du guide
     */
    @GetMapping("/me")
    public GuideResponse monProfil(
            @AuthenticationPrincipal final Jwt jwt) {

        return service.findByOwner(jwt.getSubject());
    }

    /**
     * Modifie le profil du guide authentifié.
     *
     * @param jwt token du guide connecté
     * @param request nouvelles informations du profil
     * @return profil mis à jour
     */
    @PutMapping("/me")
    public GuideResponse modifierMonProfil(
            @AuthenticationPrincipal final Jwt jwt,
            @Valid @RequestBody final GuideRequest request) {

        return service.updateMine(jwt.getSubject(), request);
    }

    /**
     * Retourne la photo associée à un guide.
     *
     * @param id identifiant du guide
     * @return réponse HTTP contenant la photo
     */
    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> photo(
            @PathVariable final Long id) {

        GuidePhoto guidePhoto = service.photo(id);

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(
                                guidePhoto.getContentType()
                        )
                )
                .cacheControl(CacheControl.noCache())
                .eTag(
                        DigestUtils.md5DigestAsHex(
                                guidePhoto.getData()
                        )
                )
                .body(guidePhoto.getData());
    }

    /**
     * Ajoute ou remplace la photo du guide authentifié.
     *
     * <p>Le fichier est transmis au format multipart/form-data.
     *
     * @param jwt token du guide connecté
     * @param file photo envoyée
     * @return réponse HTTP 204 en cas de réussite
     */
    @PutMapping(
            value = "/me/photo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Void> modifierMaPhoto(
            @AuthenticationPrincipal final Jwt jwt,
            @RequestParam("file") final MultipartFile file) {

        service.modifierMaPhoto(jwt.getSubject(), file);

        return ResponseEntity.noContent().build();
    }

    /**
     * Supprime la photo du guide authentifié.
     *
     * @param jwt token du guide connecté
     * @return réponse HTTP 204 en cas de réussite
     */
    @DeleteMapping("/me/photo")
    public ResponseEntity<Void> supprimerMaPhoto(
            @AuthenticationPrincipal final Jwt jwt) {

        service.supprimerMaPhoto(jwt.getSubject());

        return ResponseEntity.noContent().build();
    }
}