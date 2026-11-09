import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AvisService } from './avis.service';

describe('AvisService', () => {
  let service: AvisService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), AvisService],
    });
    service = TestBed.inject(AvisService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('fetches public avis with filters', () => {
    service.getAvis('public', { circuitId: 3 }).subscribe();
    const req = http.expectOne('/api/avis?circuitId=3');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('fetches avis for current user (/me)', () => {
    service.getAvis('me').subscribe();
    const req = http.expectOne('/api/avis/me');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('fetches avis for admin (/admin)', () => {
    service.getAvis('admin').subscribe();
    const req = http.expectOne('/api/avis/admin');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('fetches moyenne with circuitId or guideId', () => {
    service.getMoyenne({ guideId: 4 }).subscribe();
    const req = http.expectOne('/api/avis/moyenne?guideId=4');
    expect(req.request.method).toBe('GET');
    req.flush({ cibleType: 'GUIDE', cibleId: 4, moyenne: 4.8, nombreAvis: 10 });
  });

  it('creates an avis via POST /api/avis', () => {
    const payload = { reservationId: 12, noteGuide: 5, noteCircuit: 4, commentaire: 'Super' };
    service.createAvis(payload).subscribe();
    const req = http.expectOne('/api/avis');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({ id: 99, ...payload });
  });

  it('patches avis publication state via PATCH /api/avis/:id/publication', () => {
    service.publishAvis(42, true).subscribe();
    const req = http.expectOne('/api/avis/42/publication');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ publie: true });
    req.flush({ id: 42, publie: true });
  });

  it('checks eligibility via GET /api/reservations/:id/eligibilite', () => {
    service.checkEligibility(25).subscribe();
    const req = http.expectOne('/api/reservations/25/eligibilite');
    expect(req.request.method).toBe('GET');
    req.flush({ reservationId: 25, circuitId: 1, guideId: 2, eligible: true, raison: '' });
  });
});
