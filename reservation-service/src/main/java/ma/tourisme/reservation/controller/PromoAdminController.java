package ma.tourisme.reservation.controller;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import ma.tourisme.reservation.dto.PromoRequest;
import ma.tourisme.reservation.model.CodePromo;
import ma.tourisme.reservation.repository.CodePromoRepository;

/** Gestion des codes promo : reserve au role ADMIN (voir SecurityConfig). */
@RestController
@RequestMapping("/api/reservations/admin/promo")
public class PromoAdminController {

    private final CodePromoRepository repository;

    public PromoAdminController(CodePromoRepository repository) { this.repository = repository; }

    @GetMapping
    public List<CodePromo> lister() { return repository.findAll(Sort.by("code")); }

    @PostMapping
    @Transactional
    public CodePromo creer(@Valid @RequestBody PromoRequest req) {
        String code = req.code().trim().toUpperCase();
        if (repository.existsById(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce code existe deja");
        }
        CodePromo p = new CodePromo();
        p.setCode(code);
        p.setPourcentage(req.pourcentage());
        p.setActif(true);
        p.setDateExpiration(req.dateExpiration());
        p.setUtilisationsMax(req.utilisationsMax());
        p.setUtilisations(0);
        p.setPersonnesMin(req.personnesMin());
        p.setPremiereReservationSeulement(req.premiereReservationSeulement());
        p.setBienvenue(req.bienvenue());
        if (req.bienvenue()) retirerAutresBienvenue(code);
        return repository.save(p);
    }

    @PatchMapping("/{code}/actif")
    @Transactional
    public CodePromo actif(@PathVariable String code, @RequestParam boolean actif) {
        CodePromo p = trouver(code);
        p.setActif(actif);
        return repository.save(p);
    }

    /** Un seul code de bienvenue a la fois : les autres sont retires. */
    @PatchMapping("/{code}/bienvenue")
    @Transactional
    public CodePromo bienvenue(@PathVariable String code, @RequestParam(defaultValue = "true") boolean valeur) {
        CodePromo p = trouver(code);
        p.setBienvenue(valeur);
        if (valeur) retirerAutresBienvenue(p.getCode());
        return repository.save(p);
    }

    @DeleteMapping("/{code}")
    public void supprimer(@PathVariable String code) {
        repository.delete(trouver(code));
    }

    private CodePromo trouver(String code) {
        return repository.findById(code.trim().toUpperCase())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Code introuvable"));
    }

    private void retirerAutresBienvenue(String garder) {
        List<CodePromo> autres = repository.findAll().stream()
            .filter(c -> Boolean.TRUE.equals(c.getBienvenue()) && !c.getCode().equals(garder))
            .toList();
        autres.forEach(c -> c.setBienvenue(false));
        repository.saveAll(autres);
    }
}