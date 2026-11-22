package ma.tourisme.guide.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ma.tourisme.guide.dto.GuideAdminResponse;
import ma.tourisme.guide.dto.GuideRequest;
import ma.tourisme.guide.dto.GuideResponse;
import ma.tourisme.guide.exception.NotFoundException;
import ma.tourisme.guide.model.Guide;
import ma.tourisme.guide.model.GuidePhoto;
import ma.tourisme.guide.repository.GuidePhotoRepository;
import ma.tourisme.guide.repository.GuideRepository;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service métier responsable de la gestion des guides touristiques.
 *
 * <p>Ce service assure les opérations de consultation,
 * de création, de modification et de désactivation des guides.
 * Il gère également les profils Keycloak et les photos.
 */
@Service
public class GuideService {

    /** Taille maximale autorisée pour une photo. */
    private static final long MAX_PHOTO_SIZE = 1_500_000L;

    /** Masque utilisé pour lire un octet non signé. */
    private static final int BYTE_MASK = 0xFF;

    /** Nombre minimal d'octets pour une image JPEG. */
    private static final int JPEG_HEADER_SIZE = 3;

    /** Nombre minimal d'octets pour une image PNG. */
    private static final int PNG_HEADER_SIZE = 8;

    /** Nombre minimal d'octets pour une image WebP. */
    private static final int WEBP_HEADER_SIZE = 12;

    /** Longueur des signatures RIFF et WEBP. */
    private static final int WEBP_SIGNATURE_SIZE = 4;

    /** Position de la signature WEBP. */
    private static final int WEBP_SIGNATURE_OFFSET = 8;

    /** Premier octet de la signature JPEG. */
    private static final int JPEG_MARKER = 0xFF;

    /** Deuxième octet de la signature JPEG. */
    private static final int JPEG_START_MARKER = 0xD8;

    /** Premier octet de la signature PNG. */
    private static final int PNG_FIRST_BYTE = 0x89;

    /** Deuxième octet de la signature PNG. */
    private static final int PNG_SECOND_BYTE = 0x50;

    /** Troisième octet de la signature PNG. */
    private static final int PNG_THIRD_BYTE = 0x4E;

    /** Quatrième octet de la signature PNG. */
    private static final int PNG_FOURTH_BYTE = 0x47;

    /** Repository principal des guides. */
    private final GuideRepository repository;

    /** Repository des photos de guides. */
    private final GuidePhotoRepository photoRepository;

    /**
     * Initialise le service avec ses repositories.
     *
     * @param guideRepository repository des guides
     * @param guidePhotoRepository repository des photos
     */
    public GuideService(
            final GuideRepository guideRepository,
            final GuidePhotoRepository guidePhotoRepository) {

        this.repository = guideRepository;
        this.photoRepository = guidePhotoRepository;
    }

    /**
     * Recherche les guides selon les filtres fournis.
     *
     * @param ville ville recherchée
     * @param langue langue recherchée
     * @return liste des guides correspondants
     */
    @Transactional(readOnly = true)
    public List<GuideResponse> findAll(
            final String ville,
            final String langue) {

        String villeFiltre =
                (ville == null || ville.isBlank())
                        ? null : ville.trim();

        String langueFiltre =
                (langue == null || langue.isBlank())
                        ? null : langue.trim();

        return repository.rechercherGuides(villeFiltre, langueFiltre)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Recherche un guide par son identifiant.
     *
     * @param id identifiant du guide
     * @return informations du guide
     */
    @Transactional(readOnly = true)
    public GuideResponse findById(final Long id) {
        return toResponse(getOrThrow(id));
    }

    /**
     * Crée un guide depuis l'espace administrateur.
     *
     * @param request informations du nouveau guide
     * @return guide créé
     */
    @Transactional
    public GuideResponse create(final GuideRequest request) {

        Guide guide = new Guide();

        apply(guide, request);
        applyAdminFields(guide, request);

        guide.setActif(
                request.disponible() == null || request.disponible()
        );

        return toResponse(repository.save(guide));
    }

    /**
     * Modifie un guide depuis l'espace administrateur.
     *
     * @param id identifiant du guide
     * @param request nouvelles informations
     * @return guide modifié
     */
    @Transactional
    public GuideResponse update(
            final Long id,
            final GuideRequest request) {

        Guide guide = getOrThrow(id);

        apply(guide, request);
        applyAdminFields(guide, request);

        if (request.disponible() != null) {
            guide.setActif(request.disponible());
        }

        return toResponse(repository.save(guide));
    }

    /**
     * Retourne tous les guides pour l'administration.
     *
     * @return liste complète des guides
     */
    @Transactional(readOnly = true)
    public List<GuideAdminResponse> findAllForAdmin() {

        return repository.findAll(Sort.by("nom", "id"))
                .stream()
                .map(this::toAdminResponse)
                .toList();
    }

    /**
     * Recherche le profil associé à un compte Keycloak.
     *
     * @param subject identifiant Keycloak
     * @return profil du guide connecté
     */
    @Transactional(readOnly = true)
    public GuideResponse findByOwner(final String subject) {
        return toResponse(getByOwnerOrThrow(subject));
    }

    /**
     * Modifie uniquement le profil du guide connecté.
     *
     * @param subject identifiant Keycloak
     * @param request nouvelles informations du profil
     * @return profil mis à jour
     */
    @Transactional
    public GuideResponse updateMine(
            final String subject,
            final GuideRequest request) {

        Guide guide = getByOwnerOrThrow(subject);

        apply(guide, request);

        return toResponse(repository.save(guide));
    }

    /**
     * Recherche le guide correspondant à un compte Keycloak.
     *
     * @param subject identifiant Keycloak
     * @return entité du guide
     */
    private Guide getByOwnerOrThrow(final String subject) {

        return repository.findByOwnerSubject(subject)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucun profil guide lié à ce compte"
                ));
    }

    /**
     * Applique les informations réservées à l'administrateur.
     *
     * @param guide guide à modifier
     * @param request informations reçues
     */
    private void applyAdminFields(
            final Guide guide,
            final GuideRequest request) {

        String subject = trimOrNull(request.ownerSubject());

        if (subject != null) {
            subject = subject.toLowerCase(Locale.ROOT);

            boolean taken = guide.getId() == null
                    ? repository.existsByOwnerSubject(subject)
                    : repository.existsByOwnerSubjectAndIdNot(
                            subject,
                            guide.getId()
                    );

            if (taken) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Ce compte est déjà lié à un autre guide"
                );
            }
        }

        guide.setOwnerSubject(subject);
    }

    /**
     * Désactive un guide sans supprimer son enregistrement.
     *
     * @param id identifiant du guide
     */
    @Transactional
    public void desactiver(final Long id) {

        Guide guide = getOrThrow(id);

        guide.setActif(false);
        repository.save(guide);
    }

    /**
     * Recherche une entité Guide ou déclenche une exception.
     *
     * @param id identifiant du guide
     * @return entité trouvée
     */
    private Guide getOrThrow(final Long id) {

        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Guide avec id " + id + " introuvable"
                ));
    }

    /**
     * Applique les informations communes au profil du guide.
     *
     * @param guide guide à modifier
     * @param request données à appliquer
     */
    private void apply(
            final Guide guide,
            final GuideRequest request) {

        guide.setNom(request.nom().trim());
        guide.setPrenom(trimOrNull(request.prenom()));
        guide.setVille(trimOrNull(request.ville()));
        guide.setEmail(trimOrNull(request.email()));
        guide.setTelephone(trimOrNull(request.telephone()));
        guide.setTarifParJour(request.tarifParJour());

        guide.getLangues().clear();

        if (request.langues() != null) {
            guide.getLangues().addAll(
                    request.langues().stream()
                            .filter(langue ->
                                    langue != null
                                            && !langue.isBlank())
                            .map(String::trim)
                            .distinct()
                            .toList()
            );
        }
    }

    /**
     * Supprime les espaces inutiles d'une chaîne.
     *
     * @param value chaîne à normaliser
     * @return chaîne normalisée ou null
     */
    private String trimOrNull(final String value) {
        return value == null || value.isBlank()
                ? null : value.trim();
    }

    /**
     * Convertit une entité Guide en réponse publique.
     *
     * @param guide entité à convertir
     * @return réponse publique du guide
     */
    private GuideResponse toResponse(final Guide guide) {

        String photo = photoRepository.existsById(guide.getId())
                ? "/api/guides/" + guide.getId() + "/photo"
                : null;

        return new GuideResponse(
                guide.getId(),
                guide.getNom(),
                guide.getPrenom(),
                guide.getVille(),
                new ArrayList<>(guide.getLangues()),
                guide.getEmail(),
                guide.getTelephone(),
                guide.getTarifParJour(),
                guide.isActif(),
                guide.getNoteMoyenne(),
                photo
        );
    }

    /**
     * Convertit une entité en réponse administrateur.
     *
     * @param guide entité à convertir
     * @return réponse contenant les informations administratives
     */
    private GuideAdminResponse toAdminResponse(
            final Guide guide) {

        return new GuideAdminResponse(
                guide.getId(),
                guide.getNom(),
                guide.getPrenom(),
                guide.getVille(),
                new ArrayList<>(guide.getLangues()),
                guide.getEmail(),
                guide.getTelephone(),
                guide.getTarifParJour(),
                guide.isActif(),
                guide.isActif(),
                guide.getOwnerSubject(),
                guide.getNoteMoyenne()
        );
    }

    /**
     * Recherche la photo associée à un guide.
     *
     * @param guideId identifiant du guide
     * @return photo du guide
     */
    @Transactional(readOnly = true)
    public GuidePhoto photo(final Long guideId) {

        getOrThrow(guideId);

        return photoRepository.findById(guideId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Photo introuvable"
                ));
    }

    /**
     * Ajoute ou remplace la photo du guide connecté.
     *
     * @param subject identifiant Keycloak du guide
     * @param file fichier image envoyé
     */
    @Transactional
    public void modifierMaPhoto(
            final String subject,
            final MultipartFile file) {

        Guide guide = getByOwnerOrThrow(subject);

        if (file == null
                || file.isEmpty()
                || file.getSize() > MAX_PHOTO_SIZE) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Photo vide ou trop volumineuse"
            );
        }

        String type = file.getContentType();

        if (type == null
                || !List.of(
                        "image/jpeg",
                        "image/png",
                        "image/webp"
                ).contains(type)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Format de photo non autorise"
            );
        }

        try {
            byte[] data = file.getBytes();

            boolean valide = switch (type) {
                case "image/jpeg" -> isJpeg(data);
                case "image/png" -> isPng(data);
                case "image/webp" -> isWebp(data);
                default -> false;
            };

            if (!valide) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Le fichier ne correspond pas au format annonce"
                );
            }

            photoRepository.save(
                    new GuidePhoto(guide.getId(), type, data)
            );

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Impossible de lire la photo"
            );
        }
    }

    /**
     * Vérifie la signature initiale d'un fichier JPEG.
     *
     * @param data contenu binaire du fichier
     * @return true si la signature correspond
     */
    private boolean isJpeg(final byte[] data) {

        return data.length >= JPEG_HEADER_SIZE
                && (data[0] & BYTE_MASK) == JPEG_MARKER
                && (data[1] & BYTE_MASK) == JPEG_START_MARKER
                && (data[2] & BYTE_MASK) == JPEG_MARKER;
    }

    /**
     * Vérifie la signature initiale d'un fichier PNG.
     *
     * @param data contenu binaire du fichier
     * @return true si la signature correspond
     */
    private boolean isPng(final byte[] data) {

        return data.length >= PNG_HEADER_SIZE
                && (data[0] & BYTE_MASK) == PNG_FIRST_BYTE
                && data[1] == PNG_SECOND_BYTE
                && data[2] == PNG_THIRD_BYTE
                && data[3] == PNG_FOURTH_BYTE;
    }

    /**
     * Vérifie les signatures RIFF et WEBP d'un fichier.
     *
     * @param data contenu binaire du fichier
     * @return true si les signatures correspondent
     */
    private boolean isWebp(final byte[] data) {

        return data.length >= WEBP_HEADER_SIZE
                && new String(
                        data,
                        0,
                        WEBP_SIGNATURE_SIZE,
                        StandardCharsets.US_ASCII
                ).equals("RIFF")
                && new String(
                        data,
                        WEBP_SIGNATURE_OFFSET,
                        WEBP_SIGNATURE_SIZE,
                        StandardCharsets.US_ASCII
                ).equals("WEBP");
    }

    /**
     * Supprime la photo du guide connecté.
     *
     * @param subject identifiant Keycloak du guide
     */
    @Transactional
    public void supprimerMaPhoto(final String subject) {

        Guide guide = getByOwnerOrThrow(subject);

        photoRepository.deleteById(guide.getId());
    }
}