package ma.tourisme.guide.service;

import ma.tourisme.guide.dto.GuideRequest;
import ma.tourisme.guide.dto.GuideResponse;
import ma.tourisme.guide.exception.NotFoundException;
import ma.tourisme.guide.model.Guide;
import ma.tourisme.guide.model.GuidePhoto;
import ma.tourisme.guide.repository.GuideRepository;
import ma.tourisme.guide.repository.GuidePhotoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GuideServiceTest {

    private static final String SUB =
        "3f2a1b9c-0d4e-4f5a-8b6c-7d8e9f0a1b2c";

    @Mock
    private GuideRepository repository;

    @Mock
    private GuidePhotoRepository photoRepository;

    @InjectMocks
    private GuideService service;

    private Guide benali;
    private Guide alaoui;

    private Guide creerGuide(
            Long id,
            String nom,
            String ville,
            String... langues) {

        Guide g = new Guide();

        ReflectionTestUtils.setField(g, "id", id);

        g.setNom(nom);
        g.setVille(ville);
        g.getLangues().addAll(List.of(langues));

        return g;
    }

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
            1L, "Benali", "Agadir",
            "français", "anglais"
        );

        alaoui = creerGuide(
            2L, "Alaoui", "Marrakech",
            "français", "arabe"
        );

        when(repository.rechercherGuides(null, null))
            .thenReturn(List.of(benali, alaoui));

        when(repository.findById(1L))
            .thenReturn(Optional.of(benali));

        when(repository.findById(2L))
            .thenReturn(Optional.of(alaoui));

        when(repository.findById(99L))
            .thenReturn(Optional.empty());

        when(repository.save(any(Guide.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Par défaut, les guides n'ont pas de photo.
        when(photoRepository.existsById(any()))
            .thenReturn(false);
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
        assertNull(r.photo());
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

        assertTrue(
            service.findAll("Fès", null).isEmpty()
        );
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
        assertEquals(
            450.0,
            benali.getTarifParJour(),
            0.001
        );
        assertEquals(
            List.of("français"),
            r.langues()
        );

        verify(repository).save(benali);
    }

    @Test
    void testModifierGuideInexistant() {

        assertThrows(
            NotFoundException.class,
            () -> service.update(
                99L,
                requete("Inconnu")
            )
        );
    }

    // ==========================================
    // TESTS DE CREATION
    // ==========================================

    @Test
    void testCreerGuide() {

        GuideResponse r =
            service.create(requete("Nouveau"));

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

    // ==========================================
    // TESTS KEYCLOAK
    // ==========================================

    @Test
    void testMonProfilTrouveParLeCompteKeycloak() {

        benali.setOwnerSubject(SUB);

        when(repository.findByOwnerSubject(SUB))
            .thenReturn(Optional.of(benali));

        assertEquals(
            1L,
            service.findByOwner(SUB).id()
        );
    }

    @Test
    void testMonProfilSansCompteLieRenvoie404() {

        when(repository.findByOwnerSubject(SUB))
            .thenReturn(Optional.empty());

        ResponseStatusException erreur =
            assertThrows(
                ResponseStatusException.class,
                () -> service.findByOwner(SUB)
            );

        assertEquals(
            HttpStatus.NOT_FOUND.value(),
            erreur.getStatusCode().value()
        );
    }

    @Test
    void testCompteDejaLieAUnAutreGuideRenvoie409() {

        when(repository.existsByOwnerSubject(SUB))
            .thenReturn(true);

        GuideRequest req = new GuideRequest(
            "Nouveau",
            null,
            "Agadir",
            List.of("français"),
            null,
            null,
            300.0,
            SUB,
            true
        );

        ResponseStatusException erreur =
            assertThrows(
                ResponseStatusException.class,
                () -> service.create(req)
            );

        assertEquals(
            HttpStatus.CONFLICT.value(),
            erreur.getStatusCode().value()
        );

        verify(repository, never())
            .save(any(Guide.class));
    }

    @Test
    void testLeGuideNePeutPasChangerSonCompteNiSaDisponibilite() {

        benali.setOwnerSubject(SUB);
        benali.setActif(true);

        when(repository.findByOwnerSubject(SUB))
            .thenReturn(Optional.of(benali));

        GuideRequest req = new GuideRequest(
            "Benali",
            "Youssef",
            "Agadir",
            List.of("français"),
            null,
            null,
            500.0,
            "11111111-2222-3333-4444-555555555555",
            false
        );

        service.updateMine(SUB, req);

        assertEquals(
            SUB,
            benali.getOwnerSubject()
        );

        assertTrue(benali.isActif());

        assertEquals(
            500.0,
            benali.getTarifParJour(),
            0.001
        );
    }

    // ==========================================
    // NOUVEAUX TESTS DES PHOTOS
    // ==========================================

    @Test
    void testGuideAvecPhotoRetourneSonLien() {

        when(photoRepository.existsById(1L))
            .thenReturn(true);

        GuideResponse r = service.findById(1L);

        assertEquals(
            "/api/guides/1/photo",
            r.photo()
        );

        verify(photoRepository)
            .existsById(1L);
    }

    @Test
    void testLecturePhotoExistante() {

        byte[] data = new byte[] {
            (byte) 0xFF,
            (byte) 0xD8,
            (byte) 0xFF
        };

        GuidePhoto photo = new GuidePhoto(
            1L,
            "image/jpeg",
            data
        );

        when(photoRepository.findById(1L))
            .thenReturn(Optional.of(photo));

        GuidePhoto result = service.photo(1L);

        assertEquals(
            "image/jpeg",
            result.getContentType()
        );

        assertArrayEquals(
            data,
            result.getData()
        );
    }

    @Test
    void testLecturePhotoInexistanteRenvoie404() {

        when(photoRepository.findById(1L))
            .thenReturn(Optional.empty());

        ResponseStatusException erreur =
            assertThrows(
                ResponseStatusException.class,
                () -> service.photo(1L)
            );

        assertEquals(
            404,
            erreur.getStatusCode().value()
        );
    }

    @Test
    void testModificationPhotoDuGuideConnecte() {

        benali.setOwnerSubject(SUB);

        when(repository.findByOwnerSubject(SUB))
            .thenReturn(Optional.of(benali));

        byte[] image = new byte[] {
            (byte) 0xFF,
            (byte) 0xD8,
            (byte) 0xFF,
            0x00
        };

        MockMultipartFile fichier =
            new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                image
            );

        service.modifierMaPhoto(SUB, fichier);

        verify(photoRepository).save(
            argThat(photo ->
                photo.getGuideId().equals(1L) &&
                photo.getContentType().equals("image/jpeg") &&
                java.util.Arrays.equals(
                    photo.getData(),
                    image
                )
            )
        );
    }

    @Test
    void testSuppressionPhotoDuGuideConnecte() {

        benali.setOwnerSubject(SUB);

        when(repository.findByOwnerSubject(SUB))
            .thenReturn(Optional.of(benali));

        service.supprimerMaPhoto(SUB);

        verify(photoRepository).deleteById(1L);
    }
}
