import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CircuitNames } from './circuit-names';

describe('Circuit names', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('shows the circuit title once loaded, and the number before or if unknown', () => {
    const names = TestBed.inject(CircuitNames);
    expect(names.name(2)).toBe('Circuit n° 2');
    http.expectOne('/api/circuits').flush([{ id: 2, titre: 'Souk El Had et front de mer' }]);
    expect(names.name(2)).toBe('Souk El Had et front de mer');
    expect(names.name(9)).toBe('Circuit n° 9');
  });

  it('keeps the numbers when the Circuit API is unavailable', () => {
    const names = TestBed.inject(CircuitNames);
    http.expectOne('/api/circuits').flush({}, { status: 503, statusText: 'Unavailable' });
    expect(names.name(2)).toBe('Circuit n° 2');
  });
});
