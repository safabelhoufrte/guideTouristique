package ma.tourisme.circuit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import ma.tourisme.circuit.client.GuideClient;
import ma.tourisme.circuit.dto.CircuitRequest;
import ma.tourisme.circuit.model.Circuit;
import ma.tourisme.circuit.repository.CircuitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Tests de la logique métier seule, sans Spring, base de données ni réseau. */
@ExtendWith(MockitoExtension.class)
class CircuitServiceTest {
  @Mock CircuitRepository repository;
  @Mock GuideClient guideClient;
  @InjectMocks CircuitService service;

  private Circuit circuit(long id) {
    return new Circuit(id, "Tour Agadir", "Visite guidée", "Agadir", 4, new BigDecimal("300.50"), 7);
  }

  private CircuitRequest request(BigDecimal prix) {
    return new CircuitRequest("Kasbah", "Visite de la Kasbah", "Agadir", 3, prix, 7L);
  }

  private void assertStatus(int status, org.junit.jupiter.api.function.Executable action) {
    var error = assertThrows(ResponseStatusException.class, action);
    assertEquals(status, error.getStatusCode().value());
  }

  @Test
  void testListerTransmetFiltresEtConvertitLesChamps() {
    when(repository.findAll("Agadir", 7L)).thenReturn(List.of(circuit(1)));
    var rows = service.list("Agadir", 7L);
    assertEquals(1, rows.size());
    assertEquals(1, rows.getFirst().id());
    assertEquals("Tour Agadir", rows.getFirst().titre());
    assertEquals(new BigDecimal("300.50"), rows.getFirst().prix());
    assertEquals(7, rows.getFirst().guideId());
    verify(repository).findAll("Agadir", 7L);
    verifyNoInteractions(guideClient);
  }

  @Test
  void testListerSansResultatRenvoieListeVide() {
    when(repository.findAll(null, null)).thenReturn(List.of());
    assertTrue(service.list(null, null).isEmpty());
    verifyNoInteractions(guideClient);
  }

  @Test
  void testConsulterCircuitExistant() {
    when(repository.findById(1)).thenReturn(Optional.of(circuit(1)));
    var row = service.get(1);
    assertNotNull(row);
    assertEquals(1, row.id());
    assertEquals("Agadir", row.ville());
    assertEquals(4, row.dureeHeures());
    verifyNoInteractions(guideClient);
  }

  @Test
  void testConsulterCircuitInexistantRenvoie404() {
    when(repository.findById(99)).thenReturn(Optional.empty());
    var error = assertThrows(ResponseStatusException.class, () -> service.get(99));
    assertEquals(404, error.getStatusCode().value());
    assertEquals("Circuit introuvable", error.getReason());
  }

  @Test
  void testConsulterAncienCircuitConserveChampsInconnus() {
    when(repository.findById(1)).thenReturn(Optional.of(
        new Circuit(1, "Ancien circuit", null, null, null, new BigDecimal("100.00"), 7)));
    var row = service.get(1);
    assertEquals("Ancien circuit", row.titre());
    assertNull(row.description());
    assertNull(row.ville());
    assertNull(row.dureeHeures());
  }

  @Test
  void testCreerVerifieGuideEtTransmetTousLesChamps() {
    var req = request(new BigDecimal("250.00"));
    when(repository.insert(any())).thenAnswer(invocation -> {
      Circuit c = invocation.getArgument(0);
      return new Circuit(12, c.titre(), c.description(), c.ville(), c.dureeHeures(), c.prix(), c.guideId());
    });
    var result = service.create(req);
    var captured = ArgumentCaptor.forClass(Circuit.class);
    var order = inOrder(guideClient, repository);
    order.verify(guideClient).requireExisting(7);
    order.verify(repository).insert(captured.capture());
    Circuit model = captured.getValue();
    assertEquals(new Circuit(0, "Kasbah", "Visite de la Kasbah", "Agadir", 3,
        new BigDecimal("250.00"), 7), model);
    assertEquals(12, result.id());
    assertEquals("Kasbah", result.titre());
  }

  @Test
  void testCreerCircuitGratuitConservePrixZero() {
    when(repository.insert(any())).thenAnswer(invocation -> invocation.getArgument(0));
    var result = service.create(request(new BigDecimal("0.00")));
    assertEquals(new BigDecimal("0.00"), result.prix());
    verify(guideClient).requireExisting(7);
  }

  @Test
  void testCreerGuideInexistantNInsereRien() {
    doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Guide introuvable"))
        .when(guideClient).requireExisting(7);
    assertStatus(400, () -> service.create(request(new BigDecimal("250.00"))));
    verifyNoInteractions(repository);
  }

  @Test
  void testCreerPanneGuideNInsereRien() {
    doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE))
        .when(guideClient).requireExisting(7);
    assertStatus(503, () -> service.create(request(new BigDecimal("250.00"))));
    verifyNoInteractions(repository);
  }

  @Test
  void testModifierVerifieCircuitPuisGuideEtConserveId() {
    when(repository.findById(1)).thenReturn(Optional.of(circuit(1)));
    when(repository.update(any())).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));
    var result = service.update(1, request(new BigDecimal("250.00")));
    var captured = ArgumentCaptor.forClass(Circuit.class);
    var order = inOrder(repository, guideClient);
    order.verify(repository).findById(1);
    order.verify(guideClient).requireExisting(7);
    order.verify(repository).update(captured.capture());
    assertEquals(new Circuit(1, "Kasbah", "Visite de la Kasbah", "Agadir", 3,
        new BigDecimal("250.00"), 7), captured.getValue());
    assertEquals(1, result.id());
    assertEquals("Kasbah", result.titre());
  }

  @Test
  void testModifierCircuitInexistantNeConsultePasGuide() {
    when(repository.findById(99)).thenReturn(Optional.empty());
    assertStatus(404, () -> service.update(99, request(new BigDecimal("250.00"))));
    verifyNoInteractions(guideClient);
    verify(repository, never()).update(any());
  }

  @Test
  void testModifierGuideInexistantNeModifieRien() {
    when(repository.findById(1)).thenReturn(Optional.of(circuit(1)));
    doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST))
        .when(guideClient).requireExisting(7);
    assertStatus(400, () -> service.update(1, request(new BigDecimal("250.00"))));
    verify(repository, never()).update(any());
  }

  @Test
  void testModifierPanneGuideNeModifieRien() {
    when(repository.findById(1)).thenReturn(Optional.of(circuit(1)));
    doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE))
        .when(guideClient).requireExisting(7);
    assertStatus(503, () -> service.update(1, request(new BigDecimal("250.00"))));
    verify(repository, never()).update(any());
  }

  @Test
  void testModifierCircuitArchiveEntreLectureEtEcritureRenvoie404() {
    when(repository.findById(1)).thenReturn(Optional.of(circuit(1)));
    when(repository.update(any())).thenReturn(Optional.empty());
    assertStatus(404, () -> service.update(1, request(new BigDecimal("250.00"))));
  }

  @Test
  void testArchiverCircuitExistant() {
    when(repository.archive(1)).thenReturn(true);
    assertDoesNotThrow(() -> service.archive(1));
    verify(repository).archive(1);
    verifyNoMoreInteractions(repository);
    verifyNoInteractions(guideClient);
  }

  @Test
  void testArchiverCircuitInexistantRenvoie404() {
    when(repository.archive(99)).thenReturn(false);
    assertStatus(404, () -> service.archive(99));
    verifyNoInteractions(guideClient);
  }
}
