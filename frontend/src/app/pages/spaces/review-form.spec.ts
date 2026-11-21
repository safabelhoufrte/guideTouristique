import '@angular/compiler';
import { getTestBed } from '@angular/core/testing';
import { BrowserTestingModule, platformBrowserTesting } from '@angular/platform-browser/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { ReviewForm } from './review-form';

try {
  getTestBed().initTestEnvironment(BrowserTestingModule, platformBrowserTesting());
} catch {
  // Already initialized
}

describe('Review eligibility and submission', () => {
  let http: HttpTestingController;
  let harness: RouterTestingHarness;
  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'avis/:id', component: ReviewForm }]),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });
  afterEach(() => http.verify());
  it('explains an ineligible booking without showing a submit form', async () => {
    await harness.navigateByUrl('/avis/25', ReviewForm);
    http
      .expectOne((r) => r.url === '/api/avis/eligibilite' && r.params.get('reservationId') === '25')
      .flush({ eligible: false, reservations: [] });
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain(
      'après la date d’une visite acceptée',
    );
    expect(harness.routeNativeElement?.querySelector('form')).toBeNull();
  });
  it('blocks duplicate submissions and sends no client-supplied author or guide', async () => {
    const component = await harness.navigateByUrl('/avis/25', ReviewForm);
    http
      .expectOne((r) => r.url === '/api/avis/eligibilite' && r.params.get('reservationId') === '25')
      .flush({ eligible: true, reservations: [{ reservationId: 25, circuitId: 10, guideId: 1 }] });
    component.form.patchValue({ commentaire: 'Une belle visite !' });
    component.save();
    component.save();
    const req = http.expectOne('/api/avis');
    expect(req.request.body).toEqual({
      reservationId: 25,
      noteGuide: 5,
      noteCircuit: 5,
      commentaire: 'Une belle visite !',
    });
    req.flush({ id: 1 });
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Merci pour votre avis');
  });
  it('shows a permission failure without fabricating reservation data', async () => {
    await harness.navigateByUrl('/avis/25', ReviewForm);
    http
      .expectOne((r) => r.url === '/api/avis/eligibilite' && r.params.get('reservationId') === '25')
      .flush({}, { status: 403, statusText: 'Forbidden' });
    harness.detectChanges();
    expect(harness.routeNativeElement?.querySelector('[role="alert"]')?.textContent).toContain(
      'permissions',
    );
    expect(harness.routeNativeElement?.querySelector('form')).toBeNull();
  });
});
