import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from '../../app.routes';
import { AuthService } from '../../auth/auth.service';
import { Reservation } from '../../models/reservation';
import { Avis } from '../../services/workspace.service';
import { activityAnalytics } from './admin-home.data';
import { AdminHome } from './admin-home';

function booking(
  id: number,
  created: string,
  status: Reservation['statut'],
  price = 100,
  circuitId = 1,
): Reservation {
  return {
    id,
    circuitId,
    guideId: 7,
    dateCreation: created,
    statut: status,
    dateVisite: '2026-12-15',
    nbPersonnes: 2,
    prixTotal: price,
    nom: 'Voyageur',
    email: 'test@example.com',
  };
}
function review(
  id: number,
  created: string,
  published: boolean,
  circuit: number,
  guide: number,
): Avis {
  return {
    id,
    dateCreation: created,
    publie: published,
    noteCircuit: circuit,
    noteGuide: guide,
    reservationId: id,
    circuitId: 1,
    guideId: 7,
    auteur: 'Voyageur',
    commentaire: 'Belle visite',
  };
}

describe('Activity analytics calculations', () => {
  it('includes calendar boundaries and excludes future and older records', () => {
    const result = activityAnalytics(
      [
        booking(1, '2026-10-13T23:59:59', 'EN_ATTENTE'),
        booking(2, '2026-10-14T00:00:00', 'EN_ATTENTE'),
        booking(3, '2026-11-12T23:59:59', 'ACCEPTEE'),
        booking(4, '2026-11-13T00:00:00', 'ACCEPTEE'),
      ],
      [],
      30,
      '2026-11-12',
    );
    expect(result.from).toBe('2026-10-14');
    expect(result.bookings).toBe(2);
    expect(result.months.map((m) => m.count)).toEqual([1, 1]);
  });

  it('uses accepted historical amounts and participants without floating-point sum drift', () => {
    const result = activityAnalytics(
      [
        booking(1, '2026-11-01', 'ACCEPTEE', 0.1),
        booking(2, '2026-11-02', 'ACCEPTEE', 0.2),
        booking(3, '2026-11-03', 'EN_ATTENTE', 500),
        booking(4, '2026-11-04', 'ANNULEE', 900),
      ],
      [],
      30,
      '2026-11-12',
    );
    expect(result.acceptedAmount).toBe(0.3);
    expect(result.participants).toBe(4);
    expect(result.cancellationRate).toBe(0.25);
    expect(result.statuses.map((s) => s.count)).toEqual([1, 2, 0, 1]);
  });

  it('averages only published reviews created within the selected period', () => {
    const result = activityAnalytics(
      [],
      [
        review(1, '2026-11-01', true, 5, 4),
        review(2, '2026-11-02', true, 4, 3),
        review(3, '2026-11-03', false, 1, 1),
        review(4, '2026-09-01', true, 1, 1),
      ],
      30,
      '2026-11-12',
    );
    expect(result.publishedReviews).toBe(2);
    expect(result.circuitRating).toBe(4.5);
    expect(result.guideRating).toBe(3.5);
  });

  it('retains empty months and avoids undefined rates and ratings for no activity', () => {
    const result = activityAnalytics([], [], 90, '2026-11-12');
    expect(result.months.map((m) => m.count)).toEqual([0, 0, 0, 0]);
    expect(result.cancellationRate).toBeNull();
    expect(result.circuitRating).toBeNull();
    expect(result.topCircuits).toEqual([]);
  });

  it('ranks demand independently of the active catalogue and counts accepted requests separately', () => {
    const result = activityAnalytics(
      [
        booking(1, '2026-11-01', 'ANNULEE', 100, 99),
        booking(2, '2026-11-01', 'ACCEPTEE', 100, 99),
        booking(3, '2026-11-01', 'EN_ATTENTE', 100, 1),
      ],
      [],
      30,
      '2026-11-12',
    );
    expect(result.topCircuits).toEqual([
      { id: 99, count: 2, accepted: 1 },
      { id: 1, count: 1, accepted: 0 },
    ]);
  });
});

describe('Admin analytics page', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  function finish(reservations: Reservation[] = []) {
    http.expectOne('/api/reservations').flush(reservations);
    http.expectOne('/api/avis/admin').flush([]);
    http.expectOne('/api/circuits').flush([{ id: 1, titre: 'Kasbah' }]);
    http.expectOne('/api/guides/admin').flush([{ id: 7, actif: true }]);
  }

  it('shows honest empty states and keeps catalogue counts independent of the date filter', () => {
    const fixture = TestBed.createComponent(AdminHome);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Chargement des statistiques');
    finish();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Aucune réservation créée');
    expect(fixture.nativeElement.textContent).toContain('Aucun avis publié');
    const select = fixture.nativeElement.querySelector('select') as HTMLSelectElement;
    select.value = '30';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(fixture.componentInstance.period()).toBe(30);
    expect(fixture.componentInstance.data()!.circuits.rows.length).toBe(1);
    http.expectNone('/api/reservations');
  });

  it('keeps a failed source unavailable rather than presenting zero activity', () => {
    const fixture = TestBed.createComponent(AdminHome);
    http.expectOne('/api/reservations').flush({}, { status: 503, statusText: 'Unavailable' });
    http.expectOne('/api/avis/admin').flush([]);
    http.expectOne('/api/circuits').flush([]);
    http.expectOne('/api/guides/admin').flush([{ id: 7 }]);
    fixture.detectChanges();
    const values = [...fixture.nativeElement.querySelectorAll('.stat strong')].map((node: any) =>
      node.textContent.trim(),
    );
    expect(values).toEqual(['—', '—', '—']);
    expect(fixture.nativeElement.querySelector('.notice').textContent).toContain('Réservations');
    expect(fixture.nativeElement.querySelector('.analytics-empty')?.textContent).not.toContain(
      'Aucune réservation',
    );
    fixture.componentInstance.reload();
    finish();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.notice')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Aucune réservation créée');
  });

  it('renders chart data and ranking from real responses with an accessible table alternative', () => {
    const fixture = TestBed.createComponent(AdminHome);
    const now = new Date();
    const parts = new Intl.DateTimeFormat('en-CA', {
      timeZone: 'Africa/Casablanca',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).formatToParts(now);
    const today = ['year', 'month', 'day']
      .map((type) => parts.find((part) => part.type === type)!.value)
      .join('-');
    finish([booking(1, today, 'ACCEPTEE', 250)]);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.chart-data table')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.ranking-table').textContent).toContain('Kasbah');
    expect(fixture.componentInstance.report()!.acceptedAmount).toBe(250);
  });
});

describe('Analytics access', () => {
  it('denies a signed-in tourist the actual admin analytics route before requesting data', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter(routes),
        { provide: AuthService, useValue: { authenticated: () => true, has: () => false } },
      ],
    });
    const http = TestBed.inject(HttpTestingController);
    await RouterTestingHarness.create('/administration');
    expect(TestBed.inject(Router).url).toBe('/acces-refuse');
    http.expectNone('/api/reservations');
    http.expectNone('/api/avis/admin');
    http.verify();
  });
});
