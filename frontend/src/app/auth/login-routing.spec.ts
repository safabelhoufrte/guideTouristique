import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from '../app.routes';
import { AuthService } from './auth.service';
import { of } from 'rxjs';
import { CircuitService } from '../services/circuit.service';
import { WorkspaceService } from '../services/workspace.service';

describe('Landing and sign-in routes', () => {
  const auth = {
    authenticated: signal(false),
    problem: signal(''),
    login: vi.fn(async (_destination: string) => {}),
    home: () => '/espace-touriste',
  };
  beforeEach(() => {
    auth.login.mockReset();
    auth.problem.set('');
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        { provide: AuthService, useValue: auth },
        { provide: CircuitService, useValue: { list: () => of([]) } },
        { provide: WorkspaceService, useValue: { avis: () => of([]) } },
      ],
    });
  });

  it('shows the landing page only at the root without starting sign-in', async () => {
    const harness = await RouterTestingHarness.create('/');
    expect(harness.routeNativeElement?.querySelector('#welcome-title')?.textContent).toContain(
      'Le Maroc,',
    );
    expect(auth.login).not.toHaveBeenCalled();
  });

  it('starts sign-in at /connexion without rendering the landing page', async () => {
    const harness = await RouterTestingHarness.create('/connexion');
    expect(auth.login).toHaveBeenCalledWith('/mon-espace');
    expect(harness.routeNativeElement?.querySelector('#welcome-title')).toBeNull();
  });

  it('preserves the destination when a protected reservation starts sign-in', async () => {
    await RouterTestingHarness.create('/reservations?circuitId=10');
    expect(auth.login).toHaveBeenCalledWith('/reservations?circuitId=10');
  });

  it('offers retry and a way home if the sign-in redirect fails', async () => {
    auth.login.mockRejectedValueOnce(new Error('Network unavailable'));
    const harness = await RouterTestingHarness.create('/connexion');
    await harness.fixture.whenStable();
    harness.detectChanges();
    expect(harness.routeNativeElement?.querySelector('[role="alert"]')?.textContent).toContain(
      'réessayer',
    );
    expect(harness.routeNativeElement?.querySelector('a')?.getAttribute('href')).toBe('/');
  });
});
