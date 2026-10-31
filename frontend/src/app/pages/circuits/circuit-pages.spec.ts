import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { CircuitDetail } from './circuit-detail';
import { CircuitList } from './circuit-list';

const circuit = {
  id: 10,
  titre: 'Tour Agadir',
  description: 'Visite de la ville',
  ville: 'Agadir',
  dureeHeures: 4,
  prix: 300.5,
  guideId: 7,
};
const guide = { id: 7, nom: 'Youssef', disponible: true, langues: ['Français'] };

describe('Circuit pages with real HTTP contracts', () => {
  let http: HttpTestingController;
  let harness: RouterTestingHarness;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'circuits', component: CircuitList },
          { path: 'circuits/:id', component: CircuitDetail },
        ]),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });
  afterEach(() => http.verify());

  it('uses API filters from the URL and renders the single associated guide', async () => {
    await harness.navigateByUrl('/circuits?ville=Agadir&guideId=7', CircuitList);
    http.expectOne('/api/circuits?ville=Agadir&guideId=7').flush([circuit]);
    http.expectOne('/api/guides').flush([guide]);
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Youssef');
    expect(harness.routeNativeElement?.textContent).toContain('Tour Agadir');
    const link = harness.routeNativeElement?.querySelector('a');
    expect(link?.getAttribute('href')).toBe('/circuits/10?ville=Agadir&guideId=7');
  });

  it('preserves circuit browsing when Guide API fails', async () => {
    await harness.navigateByUrl('/circuits', CircuitList);
    http.expectOne('/api/circuits').flush([circuit]);
    http.expectOne('/api/guides').flush({}, { status: 503, statusText: 'Unavailable' });
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Tour Agadir');
    expect(harness.routeNativeElement?.textContent).toContain(
      'informations des guides sont momentanément indisponibles',
    );
  });

  it('renders an empty result rather than fake circuits', async () => {
    await harness.navigateByUrl('/circuits?ville=Inconnue', CircuitList);
    http.expectOne('/api/circuits?ville=Inconnue').flush([]);
    http.expectOne('/api/guides').flush([]);
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Aucun circuit');
    expect(harness.routeNativeElement?.querySelector('article')).toBeNull();
  });

  it('preserves the filter and retries a failed Circuit request', async () => {
    const page = await harness.navigateByUrl('/circuits?ville=Agadir', CircuitList);
    http
      .expectOne('/api/circuits?ville=Agadir')
      .flush({}, { status: 503, statusText: 'Unavailable' });
    // forkJoin cancels the Guide request when Circuit fails.
    expect(http.expectOne('/api/guides').cancelled).toBe(true);
    harness.detectChanges();
    expect(harness.routeNativeElement?.querySelector('[role="alert"]')).not.toBeNull();
    page.retry();
    http.expectOne('/api/circuits?ville=Agadir').flush([circuit]);
    http.expectOne('/api/guides').flush([guide]);
    harness.detectChanges();
    expect(page.form.controls.ville.value).toBe('Agadir');
    expect(harness.routeNativeElement?.textContent).toContain('Tour Agadir');
  });

  it('loads one guide on the detail and carries circuitId to Reservation', async () => {
    await harness.navigateByUrl('/circuits/10', CircuitDetail);
    http.expectOne('/api/circuits/10').flush(circuit);
    http.expectOne('/api/guides/7').flush(guide);
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Youssef');
    expect(
      harness.routeNativeElement?.querySelector('a[href="/reservations?circuitId=10"]'),
    ).not.toBeNull();
  });

  it('keeps the detail visible if the guide is temporarily unavailable', async () => {
    await harness.navigateByUrl('/circuits/10', CircuitDetail);
    http.expectOne('/api/circuits/10').flush(circuit);
    http.expectOne('/api/guides/7').flush({}, { status: 503, statusText: 'Unavailable' });
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Tour Agadir');
    expect(harness.routeNativeElement?.textContent).toContain(
      'informations du guide sont momentanément indisponibles',
    );
  });

  it('does not offer booking for a guide explicitly marked unavailable', async () => {
    await harness.navigateByUrl('/circuits/10', CircuitDetail);
    http.expectOne('/api/circuits/10').flush(circuit);
    http.expectOne('/api/guides/7').flush({ ...guide, disponible: false });
    harness.detectChanges();
    expect(harness.routeNativeElement?.querySelector('button[disabled]')?.textContent).toContain(
      'Guide indisponible',
    );
    expect(harness.routeNativeElement?.querySelector('a[href^="/reservations"]')).toBeNull();
  });

  it('handles archived and invalid circuit IDs', async () => {
    await harness.navigateByUrl('/circuits/10', CircuitDetail);
    http.expectOne('/api/circuits/10').flush({}, { status: 404, statusText: 'Not Found' });
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Ce circuit n’est plus disponible');
    await TestBed.inject(Router).navigateByUrl('/circuits/not-a-number');
    harness.detectChanges();
    http.expectNone('/api/circuits/not-a-number');
    expect(harness.routeNativeElement?.textContent).toContain('Ce circuit n’est plus disponible');
  });
});
