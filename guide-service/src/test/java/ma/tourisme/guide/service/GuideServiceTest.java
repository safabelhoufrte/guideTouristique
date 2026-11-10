package ma.tourisme.guide.service;

import ma.tourisme.guide.dto.GuideRequest;
import ma.tourisme.guide.dto.GuideResponse;
import ma.tourisme.guide.exception.NotFoundException;
import ma.tourisme.guide.model.Guide;
import ma.tourisme.guide.repository.GuideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GuideServiceTest {

    @Mock
    private GuideRepository repository;

    @InjectMocks
    private GuideService service;

    private Guide benali;
    private Guide alaoui;

    private Guide creerGuide(Long id, String nom, String ville, String... langues) {
        Guide g = new Guide();
        ReflectionTestUtils.setField(g, "id", id);
        g.setNom(nom);
        g.setVille(ville);
        g.getLangues().addAll(List.of(langues));
        return g;
    }

    private GuideRequest requete(String nom) {
        return new GuideRequest(nom, "Test", "Agadir", List.of("français"),
                "test@example.com", "0600000000", 300.0);
    }

    @BeforeEach
    void initialiser() {
        benali = creerGuide(1L, "Benali", "Agadir", "français", "anglais");
        alaoui = creerGuide(2L, "Alaoui", "Marrakech", "français", "arabe");
        when(repository.findByActifTrue()).thenReturn(List.of(benali, alaoui));
        when(repository.findById(1L)).thenReturn(Optional.of(benali));
        when(repository.findById(2L)).thenReturn(Optional.of(alaoui));
        when(repository.findById(99L)).thenReturn(Optional.empty());
        when(repository.save(any(Guide.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // les tests des étapes A, B et C viennent ici

        @Test
    void testRechercherGuideExistant() {
        GuideResponse r = service.findById(1L);
        assertNotNull(r);
        assertEquals("Benali", r.nom());
    }

    @Test
    void testRechercherGuideInexistant() {
        assertThrows(NotFoundException.class, () -> service.findById(99L));
    }

    @Test
    void testNombreDeGuidesVisibles() {
        assertEquals(2, service.findAll(null, null).size());
    }

    @Test
    void testFiltrerParVille() {
        List<GuideResponse> r = service.findAll("agadir", null);
        assertEquals(1, r.size());
        assertEquals("Benali", r.get(0).nom());
    }

        @Test
    void testModifierGuideExistant() {
        GuideRequest req = new GuideRequest("Benali Modifié", "Youssef", "Agadir",
                List.of("français"), "youssef@example.com", "0600000000", 450.0);
        GuideResponse r = service.update(1L, req);
        assertEquals("Benali Modifié", r.nom());
        assertEquals(450.0, benali.getTarifParJour(), 0.001);
    }

    @Test
    void testModifierGuideInexistant() {
        assertThrows(NotFoundException.class, () -> service.update(99L, requete("Inconnu")));
    }

    @Test
    void testDesactiverGuideExistant() {
        service.desactiver(1L);
        assertFalse(benali.isActif());
        verify(repository).save(benali);
    }

    @Test
    void testDesactiverGuideInexistant() {
        assertThrows(NotFoundException.class, () -> service.desactiver(99L));
    }

    // cas limites liés aux règles de gestion
    @Test
    void testGuideSansAvisEstNonNote() {
        assertNull(service.findById(1L).noteMoyenne());
    }

    @Test
    void testFiltreSansResultat() {
        assertTrue(service.findAll("Fès", null).isEmpty());
    }

    
}