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

    // Liste : seuls les guides actifs sont visibles, avec filtres facultatifs
    @Transactional(readOnly = true)
    public List<GuideResponse> findAll(String ville, String langue) {
        return repository.findByActifTrue().stream()
                .filter(g -> ville == null || ville.isBlank() || ville.equalsIgnoreCase(g.getVille()))
                .filter(g -> langue == null || langue.isBlank()
                        || g.getLangues().stream().anyMatch(l -> l.equalsIgnoreCase(langue)))
                .map(this::toResponse)
                .toList();
    }

    // Détail : renvoie aussi un guide inactif (avec son statut), pour que les
    // autres modules puissent vérifier existence, statut et note moyenne
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

    // Règle : un guide n'est pas détruit, il est désactivé
    // (la vérification des circuits et réservations à venir viendra ensuite)
    @Transactional
    public void desactiver(Long id) {
        Guide guide = getOrThrow(id);
        guide.setActif(false);
        repository.save(guide);
    }

    private Guide getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Guide avec id " + id + " introuvable"));
    }

    private void apply(Guide g, GuideRequest r) {
        g.setNom(r.nom());
        g.setPrenom(r.prenom());
        g.setVille(r.ville());
        g.setEmail(r.email());
        g.setTelephone(r.telephone());
        g.setTarifParJour(r.tarifParJour());
        g.getLangues().clear();
        if (r.langues() != null) {
            g.getLangues().addAll(r.langues());
        }
    }

    private GuideResponse toResponse(Guide g) {
        return new GuideResponse(g.getId(), g.getNom(), g.getPrenom(), g.getVille(),
                new ArrayList<>(g.getLangues()), g.getEmail(), g.getTelephone(),
                g.getTarifParJour(), g.isActif(), g.getNoteMoyenne());
    }
}