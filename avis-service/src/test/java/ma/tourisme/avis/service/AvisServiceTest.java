package ma.tourisme.avis.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import ma.tourisme.avis.client.ReservationClient;
import ma.tourisme.avis.dto.AvisRequest;
import ma.tourisme.avis.dto.AvisResponse;
import ma.tourisme.avis.dto.MoyenneResponse;
import ma.tourisme.avis.repository.AvisRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tests unitaires de AvisService.
 * Le repository (JDBC) et le client REST (reservation-service) sont simules avec Mockito :
 * aucune base de donnees, aucun appel reseau.
 */
@ExtendWith(MockitoExtension.class)
class AvisServiceTest {

  @Mock
  private AvisRepository repo;

  @Mock
  private ReservationClient reservations;

  @InjectMocks
  private AvisService service;

  private static final ReservationClient.References REFS = new ReservationClient.References(10L, 7L);

  // ---------- Methodes utilitaires ----------

  private static AvisResponse avis(long id, boolean publie) {
    return new AvisResponse(id, 25L, 10L, 7L, "Test Tourist", 5, 4,
        "Très belle visite", LocalDateTime.of(2026, 11, 7, 10, 0), publie);
  }

  private static Jwt jwt(String nom) {
    Jwt.Builder b = Jwt.withTokenValue("token-test").header("alg", "none").subject("sub-1");
    if (nom != null) {
      b.claim("name", nom);
    }
    return b.build();
  }

  /** Requete simulee : seul reservationId() est utilise par le service. */
  private static AvisRequest requete() {
    AvisRequest r = mock(AvisRequest.class);
    when(r.reservationId()).thenReturn(25L);
    return r;
  }

  private static void assertStatus(int attendu, Executable action) {
    ResponseStatusException ex = assertThrows(ResponseStatusException.class, action);
    assertEquals(attendu, ex.getStatusCode().value());
  }

  // ---------- 3.1 Consultation ----------

  @Test
  void testListPubliqueDelegueAuRepository() {
    List<AvisResponse> attendus = List.of(avis(1, true), avis(2, true));
    when(repo.list(10L, null, false)).thenReturn(attendus);

    List<AvisResponse> resultat = service.list(10L, null, false);

    assertSame(attendus, resultat);
    verify(repo).list(10L, null, false);
  }

  @Test
  void testListAdminDelegueAuRepository() {
    List<AvisResponse> attendus = List.of(avis(1, true), avis(2, false), avis(3, true));
    when(repo.list(null, null, true)).thenReturn(attendus);

    List<AvisResponse> resultat = service.list(null, null, true);

    assertEquals(3, resultat.size());
    verify(repo).list(null, null, true);
  }

  @Test
  void testGetByIdAvisPublie() {
    AvisResponse a = avis(1, true);
    when(repo.findById(1L)).thenReturn(Optional.of(a));

    assertSame(a, service.getById(1L, false));
  }

  @Test
  void testGetByIdInexistant() {
    when(repo.findById(99L)).thenReturn(Optional.empty());

    assertStatus(404, () -> service.getById(99L, false));
  }

  @Test
  void testGetByIdMasqueNonAdmin() {
    when(repo.findById(1L)).thenReturn(Optional.of(avis(1, false)));

    assertStatus(404, () -> service.getById(1L, false));
  }

  @Test
  void testGetByIdMasqueAdmin() {
    AvisResponse masque = avis(1, false);
    when(repo.findById(1L)).thenReturn(Optional.of(masque));

    assertSame(masque, service.getById(1L, true));
  }

  @Test
  void testMineUtiliseLeSubjectDuJwt() {
    List<AvisResponse> attendus = List.of(avis(1, true));
    when(repo.mine("sub-1")).thenReturn(attendus);

    assertEquals(attendus, service.mine(jwt("Test Tourist")));
    verify(repo).mine("sub-1");
  }

  // ---------- 3.2 Creation ----------

  @Test
  void testCreerNominal() {
    AvisRequest r = requete();
    Jwt jwt = jwt("Test Tourist");
    AvisResponse cree = avis(1, true);
    when(reservations.requireEligible(25L, jwt)).thenReturn(REFS);
    when(repo.insert(r, REFS, "sub-1", "Test Tourist")).thenReturn(cree);

    AvisResponse resultat = service.create(r, jwt);

    assertSame(cree, resultat);
    verify(repo).insert(r, REFS, "sub-1", "Test Tourist");
  }

  @Test
  void testCreerSansNomUtiliseVoyageur() {
    AvisRequest r = requete();
    Jwt jwt = jwt(null);
    when(reservations.requireEligible(25L, jwt)).thenReturn(REFS);
    when(repo.insert(any(), any(), any(), any())).thenReturn(avis(1, true));

    service.create(r, jwt);

    ArgumentCaptor<String> nom = ArgumentCaptor.forClass(String.class);
    verify(repo).insert(same(r), same(REFS), eq("sub-1"), nom.capture());
    assertEquals("Voyageur", nom.getValue());
  }

  @Test
  void testCreerNomVideUtiliseVoyageur() {
    AvisRequest r = requete();
    Jwt jwt = jwt("   ");
    when(reservations.requireEligible(25L, jwt)).thenReturn(REFS);
    when(repo.insert(any(), any(), any(), any())).thenReturn(avis(1, true));

    service.create(r, jwt);

    ArgumentCaptor<String> nom = ArgumentCaptor.forClass(String.class);
    verify(repo).insert(same(r), same(REFS), eq("sub-1"), nom.capture());
    assertEquals("Voyageur", nom.getValue());
  }

  @Test
  void testCreerNomTronqueA160Caracteres() {
    AvisRequest r = requete();
    Jwt jwt = jwt("x".repeat(200));
    when(reservations.requireEligible(25L, jwt)).thenReturn(REFS);
    when(repo.insert(any(), any(), any(), any())).thenReturn(avis(1, true));

    service.create(r, jwt);

    ArgumentCaptor<String> nom = ArgumentCaptor.forClass(String.class);
    verify(repo).insert(same(r), same(REFS), eq("sub-1"), nom.capture());
    assertEquals(160, nom.getValue().length());
  }

  @Test
  void testCreerDoublonRenvoie409() {
    AvisRequest r = requete();
    Jwt jwt = jwt("Test Tourist");
    when(reservations.requireEligible(25L, jwt)).thenReturn(REFS);
    when(repo.insert(r, REFS, "sub-1", "Test Tourist"))
        .thenThrow(new DuplicateKeyException("avis_reservation_id_key"));

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> service.create(r, jwt));

    assertEquals(409, ex.getStatusCode().value());
    assertEquals("Un avis existe déjà pour cette réservation.", ex.getReason());
  }

  @Test
  void testCreerReservationNonEligibleNInsereRien() {
    AvisRequest r = requete();
    Jwt jwt = jwt("Test Tourist");
    when(reservations.requireEligible(25L, jwt))
        .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Visite non acceptée"));

    assertStatus(409, () -> service.create(r, jwt));

    verifyNoInteractions(repo);
  }

  // ---------- 3.3 Moderation ----------

  @Test
  void testPublicationExistant() {
    AvisResponse masque = avis(1, false);
    when(repo.publication(1L, false)).thenReturn(Optional.of(masque));

    assertSame(masque, service.publication(1L, false));
  }

  @Test
  void testPublicationInexistant() {
    when(repo.publication(99L, true)).thenReturn(Optional.empty());

    assertStatus(404, () -> service.publication(99L, true));
  }

  @Test
  void testSupprimerExistant() {
    when(repo.delete(1L)).thenReturn(true);

    assertDoesNotThrow(() -> service.delete(1L));
    verify(repo, times(1)).delete(1L);
  }

  @Test
  void testSupprimerInexistant() {
    when(repo.delete(99L)).thenReturn(false);

    assertStatus(404, () -> service.delete(99L));
  }

  // ---------- 3.4 Moyenne ----------

  @Test
  void testMoyenneParCircuitId() {
    MoyenneResponse m = new MoyenneResponse("CIRCUIT", 10L, 4.0, 1L);
    when(repo.calculerMoyenneCircuit(10L)).thenReturn(m);

    assertSame(m, service.moyenne(10L, null, null, null));
  }

  @Test
  void testMoyenneParGuideId() {
    MoyenneResponse m = new MoyenneResponse("GUIDE", 7L, 5.0, 1L);
    when(repo.calculerMoyenneGuide(7L)).thenReturn(m);

    assertSame(m, service.moyenne(null, 7L, null, null));
  }

  @Test
  void testMoyenneCibleTypeCircuitInsensibleALaCasse() {
    MoyenneResponse m = new MoyenneResponse("CIRCUIT", 10L, 4.0, 1L);
    when(repo.calculerMoyenneCircuit(10L)).thenReturn(m);

    assertSame(m, service.moyenne(null, null, "circuit", 10L));
  }

  @Test
  void testMoyenneCibleTypeGuide() {
    MoyenneResponse m = new MoyenneResponse("GUIDE", 7L, 5.0, 1L);
    when(repo.calculerMoyenneGuide(7L)).thenReturn(m);

    assertSame(m, service.moyenne(null, null, "GUIDE", 7L));
  }

  @Test
  void testMoyenneCircuitPrioritaireSurGuide() {
    MoyenneResponse m = new MoyenneResponse("CIRCUIT", 10L, 4.0, 1L);
    when(repo.calculerMoyenneCircuit(10L)).thenReturn(m);

    assertSame(m, service.moyenne(10L, 7L, null, null));
    verify(repo, never()).calculerMoyenneGuide(anyLong());
  }

  @Test
  void testMoyenneSansCritereRenvoie400() {
    assertStatus(400, () -> service.moyenne(null, null, null, null));

    verifyNoInteractions(repo);
  }

  @Test
  void testMoyenneCibleTypeInconnuRenvoie400() {
    assertStatus(400, () -> service.moyenne(null, null, "AUTRE", 5L));

    verifyNoInteractions(repo);
  }
}