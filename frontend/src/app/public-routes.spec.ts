import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from './app.routes';

describe('Shared public routes without authentication infrastructure', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('opens the landing page using public APIs without requesting Keycloak configuration', async () => {
    const harness = await RouterTestingHarness.create('/');
    http.expectOne('/api/circuits').flush([]);
    http.expectOne('/api/avis').flush([]);
    http.expectNone('/auth-config.json');
    harness.detectChanges();
    expect(harness.routeNativeElement?.querySelector('#welcome-title')?.textContent).toContain(
      'Le Maroc,',
    );
    expect(harness.routeNativeElement?.querySelector('a[href="/circuits"]')).not.toBeNull();
  });
  it('serves published reviews through a public-only page', async () => {
    const harness = await RouterTestingHarness.create('/avis');
    http
      .expectOne('/api/avis')
      .flush([
        {
          id: 1,
          auteur: 'Sara',
          publie: true,
          commentaire: 'Belle visite',
          noteGuide: 5,
          noteCircuit: 5,
          dateCreation: '2026-10-01',
        },
      ]);
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Sara');
    expect(harness.routeNativeElement?.textContent).not.toContain('Modérer');
  });
});
