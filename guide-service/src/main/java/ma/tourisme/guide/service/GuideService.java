package ma.tourisme.guide.service;

import ma.tourisme.guide.dto.GuideRequest;
import ma.tourisme.guide.dto.GuideResponse;
import ma.tourisme.guide.exception.NotFoundException;
import ma.tourisme.guide.model.Guide;
import ma.tourisme.guide.repository.GuideRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class GuideService {

    private final GuideRepository repository;

    public GuideService(GuideRepository repository) {
        this.repository = repository;
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

    @Transactional
    public GuideResponse create(GuideRequest request) {
        Guide guide = new Guide();
        apply(guide, request);
        guide.setActif(true);

        return toResponse(repository.save(guide));
    }

    @Transactional
    public GuideResponse update(Long id, GuideRequest request) {
        Guide guide = getOrThrow(id);
        apply(guide, request);

        return toResponse(repository.save(guide));
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
            guide.getNoteMoyenne()
        );
    }
}
