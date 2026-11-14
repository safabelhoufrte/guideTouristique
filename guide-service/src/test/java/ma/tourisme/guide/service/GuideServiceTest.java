
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
import static org.mockito.Mockito.*;
 
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GuideServiceTest {

    @Mock
    private GuideRepository repository;

    @InjectMocks
    private GuideService service;

    private Guide benali;
    private Guide alaoui;

    // Création d'un guide pour les tests
    private Guide creerGuide(
            Long id,
            String nom,
            String ville,
            String... langues
    ) {
        Guide g = new Guide();

        ReflectionTestUtils.setField(g, "id", id);

        g.setNom(nom);
        g.setVille(ville);
        g.getLangues().addAll(List.of(langues));

        return g;
    }

    // Préparation d'une requête de test
    private GuideRequest requete(String nom) {
        return new GuideRequest(
                nom,
                "Test",
                "Agadir",
                List.of("français"),
                "test@example.com",
                "0600000000",
                300.0
        );
    }

    @BeforeEach
    void initialiser() {

        benali = creerGuide(
                1L,
                "Benali",
                "Agadir",
                "français",
                "anglais"
        );

        alaoui = creerGuide(
                2L,
                "Alaoui",
                "Marrakech",
                "français",
                "arabe"
        );

        // Recherche sans filtre
        when(repository.rechercherGuides(null, null))
                .thenReturn(List.of(benali, alaoui));

        // Recherche par identifiant
        when(repository.findById(1L))
                .thenReturn(Optional.of(benali));

        when(repository.findById(2L))
                .thenReturn(Optional.of(alaoui));

        when(repository.findById(99L))
                .thenReturn(Optional.empty());

        // Simulation de l'enregistrement
        when(repository.save(any(Guide.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ==========================================
    // TESTS DE RECHERCHE
    // ==========================================

    @Test
    void testRechercherGuideExistant() {

        GuideResponse r = service.findById(1L);

        assertNotNull(r);
        assertEquals("Benali", r.nom());
        assertEquals(1L, r.id());
        assertTrue(r.disponible());
    }

    @Test
    void testRechercherGuideInexistant() {

        assertThrows(
                NotFoundException.class,
                () -> service.findById(99L)
        );
    }

    @Test
    void testNombreDeGuidesVisibles() {

        List<GuideResponse> resultats =
                service.findAll(null, null);

        assertEquals(2, resultats.size());
    }

    @Test
    void testFiltrerParVille() {

        when(repository.rechercherGuides("agadir", null))
                .thenReturn(List.of(benali));

        List<GuideResponse> resultats =
                service.findAll("agadir", null);

        assertEquals(1, resultats.size());
        assertEquals("Benali", resultats.get(0).nom());

        verify(repository).rechercherGuides("agadir", null);
    }

    @Test
    void testFiltrerParLangue() {

        when(repository.rechercherGuides(null, "anglais"))
                .thenReturn(List.of(benali));

        List<GuideResponse> resultats =
                service.findAll(null, "anglais");

        assertEquals(1, resultats.size());
        assertEquals("Benali", resultats.get(0).nom());

        verify(repository).rechercherGuides(null, "anglais");
    }

    @Test
    void testFiltrerParVilleEtLangue() {

        when(repository.rechercherGuides("agadir", "anglais"))
                .thenReturn(List.of(benali));

        List<GuideResponse> resultats =
                service.findAll("agadir", "anglais");

        assertEquals(1, resultats.size());
        assertEquals("Benali", resultats.get(0).nom());

        verify(repository).rechercherGuides("agadir", "anglais");
    }

    @Test
    void testFiltreSansResultat() {

        when(repository.rechercherGuides("Fès", null))
                .thenReturn(List.of());

        assertTrue(service.findAll("Fès", null).isEmpty());
    }

    // ==========================================
    // TESTS DE MODIFICATION
    // ==========================================

    @Test
    void testModifierGuideExistant() {

        GuideRequest req = new GuideRequest(
                "Benali Modifié",
                "Youssef",
                "Agadir",
                List.of("français"),
                "youssef@example.com",
                "0600000000",
                450.0
        );

        GuideResponse r = service.update(1L, req);

        assertEquals("Benali Modifié", r.nom());
        assertEquals(450.0, benali.getTarifParJour(), 0.001);
        assertEquals(List.of("français"), r.langues());

        verify(repository).save(benali);
    }

    @Test
    void testModifierGuideInexistant() {

        assertThrows(
                NotFoundException.class,
                () -> service.update(99L, requete("Inconnu"))
        );
    }

    // ==========================================
    // TESTS DE CREATION
    // ==========================================

    @Test
    void testCreerGuide() {

        GuideResponse r = service.create(requete("Nouveau"));

        assertNotNull(r);
        assertEquals("Nouveau", r.nom());
        assertTrue(r.disponible());

        verify(repository).save(any(Guide.class));
    }

    // ==========================================
    // TESTS DE DESACTIVATION
    // ==========================================

    @Test
    void testDesactiverGuideExistant() {

        service.desactiver(1L);

        assertFalse(benali.isActif());

        verify(repository).save(benali);
    }

    @Test
    void testDesactiverGuideInexistant() {

        assertThrows(
                NotFoundException.class,
                () -> service.desactiver(99L)
        );
    }

    // ==========================================
    // TESTS DES REGLES DE GESTION
    // ==========================================

    @Test
    void testGuideSansAvisEstNonNote() {

        GuideResponse r = service.findById(1L);

        assertNull(r.noteMoyenne());
    }

    @Test
    void testGuideSansLangues() {

        Guide guide = creerGuide(
                3L,
                "Nouveau",
                "Agadir"
        );

        when(repository.findById(3L))
                .thenReturn(Optional.of(guide));

        GuideResponse r = service.findById(3L);

        assertTrue(r.langues().isEmpty());
    }
}
