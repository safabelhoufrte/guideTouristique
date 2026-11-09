import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { CircuitService } from '../services/circuit.service';
import { Avis, WorkspaceService } from '../services/workspace.service';
import { LandingShowcase } from './landing-showcase';

describe('Landing showcase', () => {
  const circuit = {
    id: 42,
    titre: 'Les jardins de Marrakech',
    ville: 'Marrakech',
    prix: 250,
    description: 'Une promenade au jardin',
    dureeHeures: 3,
    guideId: 1,
  };
  const review: Avis = {
    id: 7,
    auteur: 'Sara Alami',
    commentaire: 'Une belle visite.',
    noteCircuit: 5,
    noteGuide: 4,
    circuitId: 42,
    guideId: 1,
    reservationId: 2,
    publie: true,
    dateCreation: '2026-10-30T12:00:00Z',
  };
  const circuitsApi = { list: vi.fn(() => of([circuit])) };
  const reviewsApi = { avis: vi.fn(() => of([review])) };
  beforeEach(() => {
    circuitsApi.list.mockReset().mockReturnValue(of([circuit]));
    reviewsApi.avis.mockReset().mockReturnValue(of([review]));
    TestBed.configureTestingModule({
      imports: [LandingShowcase],
      providers: [
        provideRouter([]),
        { provide: CircuitService, useValue: circuitsApi },
        { provide: WorkspaceService, useValue: reviewsApi },
      ],
    });
  });
  function render() {
    const fixture = TestBed.createComponent(LandingShowcase);
    fixture.detectChanges();
    return fixture;
  }

  it('shows real circuit data and navigates to the matching circuit', () => {
    const fixture = render();
    const card = fixture.nativeElement.querySelector('.circuit-card');
    expect(card.textContent).toContain(circuit.titre);
    expect(card.textContent).toContain('250');
    expect(card.querySelector('a').getAttribute('href')).toBe('/circuits/42');
    expect(card.querySelector('img').getAttribute('src')).toContain('marrakech-garden');
  });
  it('shows published reviews with their real authors and excludes hidden reviews', () => {
    reviewsApi.avis.mockReturnValue(
      of([review, { ...review, id: 8, auteur: 'Hidden author', publie: false }]),
    );
    const fixture = render();
    const cards = fixture.nativeElement.querySelector('.reviews-grid');
    expect(reviewsApi.avis).toHaveBeenCalledWith('public');
    expect(cards.textContent).toContain('Sara Alami');
    expect(cards.textContent).not.toContain('Hidden author');
    expect(fixture.nativeElement.querySelector('.sample-notice')).toBeNull();
  });
  it('clearly identifies fictional previews when there are no published reviews', () => {
    reviewsApi.avis.mockReturnValue(of([]));
    const fixture = render();
    expect(fixture.nativeElement.querySelector('.sample-notice').textContent).toContain(
      'exemples fictifs',
    );
    expect(fixture.nativeElement.querySelectorAll('.review-card')).toHaveLength(3);
    expect(fixture.nativeElement.querySelector('.review-author').textContent).toContain(
      'Exemple fictif',
    );
  });
  it('keeps the gallery and reviews available during a circuit outage and supports retry', () => {
    circuitsApi.list.mockReturnValueOnce(throwError(() => new Error('Unavailable')));
    const fixture = render();
    expect(fixture.nativeElement.querySelectorAll('.photo-gallery img')).toHaveLength(4);
    expect(fixture.nativeElement.querySelector('.reviews-grid').textContent).toContain(
      'Sara Alami',
    );
    fixture.componentInstance.reloadCircuits.next();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.circuit-card').textContent).toContain(
      circuit.titre,
    );
  });
  it('never replaces a failed reviews request with fictional testimonials', () => {
    reviewsApi.avis.mockReturnValue(throwError(() => new Error('Unavailable')));
    const fixture = render();
    expect(fixture.nativeElement.textContent).toContain(
      'Les avis sont momentanément indisponibles',
    );
    expect(fixture.nativeElement.querySelector('.reviews-grid')).toBeNull();
  });
});
