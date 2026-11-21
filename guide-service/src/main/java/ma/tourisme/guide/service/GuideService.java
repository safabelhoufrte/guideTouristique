package ma.tourisme.guide.service;

import ma.tourisme.guide.dto.GuideAdminResponse;
import ma.tourisme.guide.dto.GuideRequest;
import ma.tourisme.guide.dto.GuideResponse;
import ma.tourisme.guide.exception.NotFoundException;
import ma.tourisme.guide.model.Guide;
import ma.tourisme.guide.repository.GuideRepository;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


import ma.tourisme.guide.model.GuidePhoto;
import ma.tourisme.guide.repository.GuidePhotoRepository;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Service
public class GuideService {


    private final GuideRepository repository;
    private final GuidePhotoRepository photoRepository;

    public GuideService(
            GuideRepository repository,
            GuidePhotoRepository photoRepository) {

        this.repository = repository;
        this.photoRepository = photoRepository;
    }

    @Transactional(readOnly = true)
    public List<GuideResponse> findAll(String ville, String langue) {

        String villeFiltre =
            (ville == null || ville.isBlank()) ? null : ville.trim();

        String langueFiltre =
            (langue == null || langue.isBlank()) ? null : langue.trim();

        return repository.rechercherGuides(villeFiltre, langueFiltre)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public GuideResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    /** ADMIN: creates a guide, optionally linked to a Keycloak account. */
    @Transactional
    public GuideResponse create(GuideRequest request) {
        Guide guide = new Guide();
        apply(guide, request);
        applyAdminFields(guide, request);
        guide.setActif(request.disponible() == null || request.disponible());

        return toResponse(repository.save(guide));
    }

    /** ADMIN: updates any guide, including its account link and availability. */
    @Transactional
    public GuideResponse update(Long id, GuideRequest request) {
        Guide guide = getOrThrow(id);
        apply(guide, request);
        applyAdminFields(guide, request);
        if (request.disponible() != null) {
            guide.setActif(request.disponible());
        }

        return toResponse(repository.save(guide));
    }

    /** ADMIN: every guide, active or not, with its linked account. */
    @Transactional(readOnly = true)
    public List<GuideAdminResponse> findAllForAdmin() {
        return repository.findAll(Sort.by("nom", "id"))
            .stream()
            .map(this::toAdminResponse)
            .toList();
    }

    /** GUIDE: the profile linked to the caller's Keycloak account (token "sub"). */
    @Transactional(readOnly = true)
    public GuideResponse findByOwner(String subject) {
        return toResponse(getByOwnerOrThrow(subject));
    }

    /** GUIDE: edits only the caller's own profile fields; account link and availability stay. */
    @Transactional
    public GuideResponse updateMine(String subject, GuideRequest request) {
        Guide guide = getByOwnerOrThrow(subject);
        apply(guide, request);

        return toResponse(repository.save(guide));
    }

    private Guide getByOwnerOrThrow(String subject) {
        return repository.findByOwnerSubject(subject)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Aucun profil guide lié à ce compte"));
    }

    /** One Keycloak account can own at most one guide profile. */
    private void applyAdminFields(Guide guide, GuideRequest request) {
        String subject = trimOrNull(request.ownerSubject());
        if (subject != null) {
            subject = subject.toLowerCase(Locale.ROOT);
            boolean taken = guide.getId() == null
                ? repository.existsByOwnerSubject(subject)
                : repository.existsByOwnerSubjectAndIdNot(subject, guide.getId());
            if (taken) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Ce compte est déjà lié à un autre guide");
            }
        }
        guide.setOwnerSubject(subject);
    }

    @Transactional
    public void desactiver(Long id) {
        Guide guide = getOrThrow(id);
        guide.setActif(false);
        repository.save(guide);
    }

    private Guide getOrThrow(Long id) {
        return repository.findById(id)
            .orElseThrow(() ->
                new NotFoundException(
                    "Guide avec id " + id + " introuvable"
                )
            );
    }

    private void apply(Guide guide, GuideRequest request) {

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
                    .filter(langue -> langue != null && !langue.isBlank())
                    .map(String::trim)
                    .distinct()
                    .toList()
            );
        }
    }

    private String trimOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }


    private GuideResponse toResponse(Guide guide) {

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

    private GuideAdminResponse toAdminResponse(Guide guide) {
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

    @Transactional(readOnly = true)
    public GuidePhoto photo(Long guideId) {
        // Vérifier aussi que le guide existe.
        getOrThrow(guideId);

        return photoRepository.findById(guideId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Photo introuvable"
            ));
    }

    @Transactional
    public void modifierMaPhoto(
            String subject,
            MultipartFile file) {

        Guide guide = getByOwnerOrThrow(subject);

        if (file == null || file.isEmpty()
                || file.getSize() > 1_500_000) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Photo vide ou trop volumineuse"
            );
        }

        String type = file.getContentType();

        if (type == null || !List.of(
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

            // Vérifier les signatures réelles des images.
            boolean valide = switch (type) {
                case "image/jpeg" ->
                    data.length >= 3 &&
                    (data[0] & 0xff) == 0xff &&
                    (data[1] & 0xff) == 0xd8 &&
                    (data[2] & 0xff) == 0xff;

                case "image/png" ->
                    data.length >= 8 &&
                    (data[0] & 0xff) == 0x89 &&
                    data[1] == 0x50 &&
                    data[2] == 0x4e &&
                    data[3] == 0x47;

                case "image/webp" ->
                    data.length >= 12 &&
                    new String(data, 0, 4,
                        java.nio.charset.StandardCharsets.US_ASCII)
                        .equals("RIFF") &&
                    new String(data, 8, 4,
                        java.nio.charset.StandardCharsets.US_ASCII)
                        .equals("WEBP");

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

    @Transactional
    public void supprimerMaPhoto(String subject) {
        Guide guide = getByOwnerOrThrow(subject);
        photoRepository.deleteById(guide.getId());
    }
}
