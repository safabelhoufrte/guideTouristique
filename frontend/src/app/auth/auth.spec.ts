import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
} from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { roleGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('Authentication boundaries', () => {
  const auth = {
    authenticated: () => false,
    has: (_: string) => false,
    accessToken: vi.fn(async () => 'test-access-token'),
  };
  let http: HttpTestingController;
  beforeEach(() => {
    auth.authenticated = () => false;
    auth.has = () => false;
    auth.accessToken.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  const guard = () =>
    TestBed.runInInjectionContext(() =>
      roleGuard(
        { data: { roles: ['ADMIN'] } } as unknown as ActivatedRouteSnapshot,
        { url: '/administration/circuits' } as RouterStateSnapshot,
      ),
    );
  it('preserves the destination when an anonymous user needs to sign in', () => {
    expect(TestBed.inject(Router).serializeUrl(guard() as any)).toBe(
      '/connexion?retour=%2Fadministration%2Fcircuits',
    );
  });
  it('denies a signed-in tourist the admin workspace', () => {
    auth.authenticated = () => true;
    expect(TestBed.inject(Router).serializeUrl(guard() as any)).toBe('/acces-refuse');
  });
  it('allows the required role', () => {
    auth.authenticated = () => true;
    auth.has = (role) => role === 'ADMIN';
    expect(guard()).toBe(true);
  });
  it('adds a refreshed bearer token to a same-origin API request', async () => {
    TestBed.inject(HttpClient).get('/api/guides/me').subscribe();
    await vi.waitFor(() => expect(auth.accessToken).toHaveBeenCalledOnce());
    const req = http.expectOne('/api/guides/me');
    expect(req.request.headers.get('Authorization')).toBe('Bearer test-access-token');
    req.flush({});
  });
  it('never leaks tokens to third-party URLs or public assets', () => {
    const client = TestBed.inject(HttpClient);
    for (const url of [
      'https://example.com/api/guides',
      '//example.com/api/guides',
      '/auth-config.json',
    ]) {
      client.get(url).subscribe();
      const req = http.expectOne(url);
      expect(req.request.headers.has('Authorization')).toBe(false);
      req.flush({});
    }
    expect(auth.accessToken).not.toHaveBeenCalled();
  });
});
