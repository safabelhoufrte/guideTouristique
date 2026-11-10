import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, map, of, startWith, Subject, switchMap } from 'rxjs';
import { Circuit } from '../models/circuit';
import { circuitPhoto } from '../models/circuit-photo';
import { CircuitService } from '../services/circuit.service';
import { Avis, WorkspaceService } from '../services/workspace.service';

type ReviewCard = Pick<Avis, 'id' | 'auteur' | 'noteCircuit' | 'noteGuide' | 'commentaire'> &
  Partial<Pick<Avis, 'circuitId' | 'dateCreation'>>;

@Component({
  selector: 'app-landing-showcase',
  imports: [RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './landing-showcase.html',
  styleUrl: './landing-showcase.scss',
})
export class LandingShowcase {
  private readonly circuitsApi = inject(CircuitService);
  private readonly reviewsApi = inject(WorkspaceService);
  readonly reloadCircuits = new Subject<void>();
  readonly reloadReviews = new Subject<void>();
  readonly circuits = toSignal(
    this.reloadCircuits.pipe(
      startWith(undefined),
      switchMap(() =>
        this.circuitsApi.list().pipe(
          map((rows) => ({ rows: rows.slice(0, 4), loading: false, error: false })),
          catchError(() => of({ rows: [] as Circuit[], loading: false, error: true })),
          startWith({ rows: [] as Circuit[], loading: true, error: false }),
        ),
      ),
    ),
    { initialValue: { rows: [] as Circuit[], loading: true, error: false } },
  );
  readonly avis = toSignal(
    this.reloadReviews.pipe(
      startWith(undefined),
      switchMap(() =>
        this.reviewsApi.avis('public').pipe(
          map((rows) => ({
            rows: rows.filter((row) => row.publie).slice(0, 3),
            loading: false,
            error: false,
          })),
          catchError(() => of({ rows: [] as Avis[], loading: false, error: true })),
          startWith({ rows: [] as Avis[], loading: true, error: false }),
        ),
      ),
    ),
    { initialValue: { rows: [] as Avis[], loading: true, error: false } },
  );

  readonly gallery = [
    {
      src: '/images/agadir-coast.jpg',
      title: 'L’appel de l’Atlantique',
      caption: 'Agadir · Entre ville et océan',
      alt: 'La plage d’Agadir et la colline en arrière-plan',
    },
    {
      src: '/images/morocco-zellige.jpg',
      title: 'Le Maroc dans les détails',
      caption: 'Zellige · Un savoir-faire à admirer',
      alt: 'Fontaine décorée de zellige et de plâtre sculpté',
    },
    {
      src: '/images/marrakech-garden.jpg',
      title: 'Une parenthèse au jardin',
      caption: 'Marrakech · À l’ombre des palmiers',
      alt: 'Fontaine et plantes luxuriantes dans un jardin de Marrakech',
    },
    {
      src: '/images/morocco-kasbah.jpg',
      title: 'Les couleurs de la terre',
      caption: 'Paysages · L’envie de prendre le temps',
      alt: 'Maisons de terre ocre devant une palmeraie et les montagnes',
    },
  ];
  private readonly examples: ReviewCard[] = [
    {
      id: -1,
      auteur: 'Salma B.',
      noteCircuit: 5,
      noteGuide: 5,
      commentaire:
        'Les petites ruelles, les histoires du guide, les pauses pour regarder autour de soi… C’est ce rythme-là que j’aime en voyage.',
    },
    {
      id: -2,
      auteur: 'Yassine E.',
      noteCircuit: 4,
      noteGuide: 5,
      commentaire:
        'Une autre façon de découvrir la ville. J’ai surtout aimé les échanges et les détails qu’on ne remarque pas quand on se promène seul.',
    },
    {
      id: -3,
      auteur: 'Camille L.',
      noteCircuit: 5,
      noteGuide: 5,
      commentaire:
        'Prendre le temps de découvrir, poser des questions et repartir avec des histoires à raconter. Une belle idée pour une première visite.',
    },
  ];
  readonly reviewCards = computed<ReviewCard[]>(() =>
    this.avis().rows.length ? this.avis().rows : this.examples,
  );

  readonly photoFor = circuitPhoto;
  initials(name: string) {
    return (
      name
        .trim()
        .split(/\s+/)
        .slice(0, 2)
        .map((part) => part[0] || '')
        .join('')
        .toUpperCase() || 'V'
    );
  }
}
