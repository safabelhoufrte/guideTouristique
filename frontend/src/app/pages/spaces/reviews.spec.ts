import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Reviews } from './reviews';
import { AuthService } from '../../auth/auth.service';
import { signal } from '@angular/core';

describe('Reviews component', () => {
  let http: HttpTestingController;
  let harness: RouterTestingHarness;
  let authServiceMock: {
    has: ReturnType<typeof vi.fn>;
    login: ReturnType<typeof vi.fn>;
    authenticated: ReturnType<typeof signal>;
  };

  const sampleReview = {
    id: 1,
    reservationId: 10,
    circuitId: 2,
    guideId: 3,
    auteur: 'Karim B.',
    noteGuide: 5,
    noteCircuit: 4,
    commentaire: 'Magnifique expérience au coucher du soleil.',
    dateCreation: '2026-11-09T14:30:00',
    publie: true,
  };

  beforeEach(async () => {
    authServiceMock = {
      has: vi.fn((role: string) => role === 'USER'),
      login: vi.fn(),
      authenticated: signal(true),
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'avis', component: Reviews, data: { scope: 'public' } },
          { path: 'espace-touriste/avis', component: Reviews, data: { scope: 'me' } },
          { path: 'administration/avis', component: Reviews, data: { scope: 'admin' } },
        ]),
        { provide: AuthService, useValue: authServiceMock },
      ],
    });

    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => http.verify());

  it('renders public reviews list and stars with aria-label', async () => {
    await harness.navigateByUrl('/avis', Reviews);
    const req = http.expectOne('/api/avis');
    expect(req.request.method).toBe('GET');
    req.flush([sampleReview]);

    const resReq = http.expectOne('/api/reservations/me');
    resReq.flush([]);

    harness.detectChanges();

    const author = harness.routeNativeElement?.querySelector('.review-author');
    expect(author?.textContent).toContain('Karim B.');

    const stars = harness.routeNativeElement?.querySelectorAll('.stars');
    expect(stars?.length).toBeGreaterThan(0);
    expect(stars?.[0].getAttribute('aria-label')).toBe('5 sur 5');
    expect(stars?.[1].getAttribute('aria-label')).toBe('4 sur 5');

    const comment = harness.routeNativeElement?.querySelector('.review-text');
    expect(comment?.textContent).toContain('Magnifique expérience au coucher du soleil.');

    const date = harness.routeNativeElement?.querySelector('.review-date');
    expect(date?.textContent).toContain('09/11/2026');
  });

  it('fetches and displays moyenne when circuitId filter is present', async () => {
    await harness.navigateByUrl('/avis?circuitId=2', Reviews);
    const req = http.expectOne('/api/avis?circuitId=2');
    req.flush([sampleReview]);

    const moyReq = http.expectOne('/api/avis/moyenne?circuitId=2');
    moyReq.flush({ cibleType: 'CIRCUIT', cibleId: 2, moyenne: 4.5, nombreAvis: 8 });

    const resReq = http.expectOne('/api/reservations/me');
    resReq.flush([]);

    harness.detectChanges();

    const moyCard = harness.routeNativeElement?.querySelector('.moyenne-card');
    expect(moyCard?.textContent).toContain('4.5 / 5');
    expect(moyCard?.textContent).toContain('(8 avis)');
  });

  it('shows login button on public page when visitor is not logged in', async () => {
    authServiceMock.has.mockReturnValue(false);
    await harness.navigateByUrl('/avis', Reviews);
    http.expectOne('/api/avis').flush([]);
    harness.detectChanges();

    const loginBtn = harness.routeNativeElement?.querySelector('.login-box button');
    expect(loginBtn?.textContent).toContain('Se connecter');

    (loginBtn as HTMLButtonElement).click();
    expect(authServiceMock.login).toHaveBeenCalledWith('/avis');
  });

  it('allows admin moderation toggle via PATCH /api/avis/:id/publication', async () => {
    const comp = await harness.navigateByUrl('/administration/avis', Reviews);
    http.expectOne('/api/avis/admin').flush([sampleReview]);
    harness.detectChanges();

    comp.toggle(sampleReview);
    const patchReq = http.expectOne('/api/avis/1/publication');
    expect(patchReq.request.method).toBe('PATCH');
    expect(patchReq.request.body).toEqual({ publie: false });
    patchReq.flush({ ...sampleReview, publie: false });

    // Triggers refresh
    http.expectOne('/api/avis/admin').flush([{ ...sampleReview, publie: false }]);
  });
});
