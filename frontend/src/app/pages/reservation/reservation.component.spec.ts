import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { ReservationComponent } from './reservation.component';

const circuits = [
  { id: 1, titre: 'Premier', prix: 200, guideId: 1, dureeHeures: 2 },
  { id: 10, titre: 'Choisi', prix: 300.5, guideId: 7, dureeHeures: 4 },
];

describe('Circuit to Reservation handoff', () => {
  let http: HttpTestingController;
  let harness: RouterTestingHarness;
  beforeEach(async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([
      { path: 'reservations', component: ReservationComponent },
    ])] });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });
  afterEach(() => http.verify());

  it('preselects the requested circuit, calls its guide and sends a real POST once', async () => {
    const page = await harness.navigateByUrl('/reservations?circuitId=10', ReservationComponent);
    http.expectOne('/api/circuits').flush(circuits);
    http.expectOne('/api/guides/7').flush({ id: 7, nom: 'Guide choisi', disponible: true });
    expect(page.form.controls.circuitId.value).toBe(10);
    expect(page.guideNom).toBe('Guide choisi');
    expect(page.total).toBe(601);
    page.form.patchValue({ nom: 'Touriste test', email: 'test@example.com', dateVisite: '2099-11-10' });
    page.confirmer();
    const post = http.expectOne('/api/reservations');
    expect(post.request.method).toBe('POST');
    expect(post.request.body.circuitId).toBe(10);
    expect(post.request.body.prixTotal).toBeUndefined();
    page.confirmer();
    http.expectNone('/api/reservations');
    post.flush({ id: 25, statut: 'EN_ATTENTE', prixTotal: 601 });
    harness.detectChanges();
    expect(page.message).toContain('25');
    expect(page.confirmationId).toBe(25);
  });

  it('never silently switches a missing selected circuit to another one', async () => {
    const page = await harness.navigateByUrl('/reservations?circuitId=99', ReservationComponent);
    http.expectOne('/api/circuits').flush(circuits);
    expect(page.form.controls.circuitId.value).toBe(0);
    expect(page.erreur).toContain('plus disponible');
    http.expectNone('/api/guides/1');
  });

  it('shows a failed booking without inventing a confirmation', async () => {
    const page = await harness.navigateByUrl('/reservations?circuitId=10', ReservationComponent);
    http.expectOne('/api/circuits').flush(circuits);
    http.expectOne('/api/guides/7').flush({ id: 7, nom: 'Guide', disponible: true });
    page.form.patchValue({ nom: 'Test', email: 'test@example.com', dateVisite: '2099-11-10' });
    page.confirmer();
    http.expectOne('/api/reservations').flush({}, { status: 503, statusText: 'Unavailable' });
    expect(page.message).toBe('');
    expect(page.confirmationId).toBeNull();
    expect(page.envoi).toBe(false);
    expect(page.erreur).toContain('indisponible');
  });
});
