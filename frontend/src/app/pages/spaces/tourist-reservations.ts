import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import {
  catchError,
  finalize,
  forkJoin,
  map,
  Observable,
  of,
  startWith,
  Subject,
  switchMap,
} from 'rxjs';
import { Circuit, Guide } from '../../models/circuit';
import { circuitPhoto } from '../../models/circuit-photo';
import { Reservation } from '../../models/reservation';
import { CircuitService } from '../../services/circuit.service';
import { GuideService } from '../../services/guide.service';
import { apiError, WorkspaceService } from '../../services/workspace.service';

export type ReservationTab = 'attente' | 'confirmees' | 'annulees' | 'historique';

/** Reservation as shown on a card: API row + names/photo read from Circuit and Guide. */
export interface ReservationCard {
  row: Reservation & { heure?: string | null };
  tab: ReservationTab;
  titre: string;
  guide: string;
  photo: { src: string; alt: string };
}

/** Today in Morocco as `YYYY-MM-DD`, comparable with `dateVisite`. */
export function today(): string {
  return new Date().toLocaleDateString('en-CA', { timeZone: 'Africa/Casablanca' });
}

/**
 * Which tab a reservation belongs to.
 * `todayIso` is today's date (`YYYY-MM-DD`), so `r.dateVisite < todayIso` means the visit is over.
 */
export function tabOf(r: Reservation, todayIso: string): ReservationTab {
  // Cancelled or refused stays in "Annulées", whatever the date.
  if (r.statut === 'ANNULEE' || r.statut === 'REFUSEE') return 'annulees';
  // Visit date passed: the trip is history (done, or a request never confirmed in time).
  if (r.dateVisite < todayIso) return 'historique';
  return r.statut === 'ACCEPTEE' ? 'confirmees' : 'attente';
}

function rows<T>(source: Observable<T[]>) {
  return source.pipe(catchError(() => of([] as T[])));
}

@Component({
  selector: 'app-tourist-reservations',
  imports: [DatePipe, RouterLink],
  templateUrl: './tourist-reservations.html',
  styleUrl: './tourist-reservations.scss',
})
export class TouristReservations {
  private readonly api = inject(WorkspaceService);
  private readonly circuitsApi = inject(CircuitService);
  private readonly guidesApi = inject(GuideService);
  private readonly refresh = new Subject<void>();

  readonly tabs: { id: ReservationTab; label: string }[] = [
    { id: 'attente', label: 'En attente' },
    { id: 'confirmees', label: 'Confirmées' },
    { id: 'annulees', label: 'Annulées' },
    { id: 'historique', label: 'Historique' },
  ];
  readonly labels = {
    EN_ATTENTE: 'En attente',
    ACCEPTEE: 'Confirmée',
    REFUSEE: 'Refusée',
    ANNULEE: 'Annulée',
  };
  readonly tab = signal<ReservationTab>('attente');
  readonly search = signal('');
  readonly pending = signal<ReservationCard | null>(null);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly message = signal('');

  /** Reservations are required; circuit and guide names are optional decorations. */
  readonly vm = toSignal(
    this.refresh.pipe(
      startWith(undefined),
      switchMap(() =>
        forkJoin({
          reservations: this.api.reservations('me'),
          circuits: rows<Circuit>(this.circuitsApi.list()),
          guides: rows<Guide>(this.guidesApi.list()),
        }).pipe(
          map(({ reservations, circuits, guides }) => ({
            loading: false,
            error: '',
            cards: this.toCards(reservations, circuits, guides),
          })),
          catchError((e) => of({ loading: false, error: apiError(e), cards: [] as ReservationCard[] })),
          startWith({ loading: true, error: '', cards: [] as ReservationCard[] }),
        ),
      ),
    ),
    { initialValue: { loading: true, error: '', cards: [] as ReservationCard[] } },
  );

  readonly counts = computed(() => {
    const result = { attente: 0, confirmees: 0, annulees: 0, historique: 0 };
    for (const card of this.vm().cards) result[card.tab]++;
    return result;
  });

  readonly visible = computed(() => {
    const text = this.search().trim().toLocaleLowerCase('fr');
    return this.vm()
      .cards.filter((card) => card.tab === this.tab())
      .filter((card) => !text || `${card.titre} ${card.guide}`.toLocaleLowerCase('fr').includes(text));
  });

  private toCards(reservations: Reservation[], circuits: Circuit[], guides: Guide[]): ReservationCard[] {
    const todayIso = today();
    return [...reservations]
      .sort((a, b) => b.dateVisite.localeCompare(a.dateVisite))
      .map((row) => {
        const circuit = circuits.find((c) => c.id === row.circuitId);
        const guide = guides.find((g) => g.id === row.guideId);
        const titre = circuit?.titre ?? `Circuit n° ${row.circuitId}`;
        return {
          row,
          tab: tabOf(row, todayIso),
          titre,
          guide: guide?.nom ?? `Guide n° ${row.guideId}`,
          photo: circuitPhoto({ titre, ville: circuit?.ville ?? null }),
        };
      });
  }

  /** Always group thousands ("1 960 MAD"), like the mockup; fr-MA skips it for 4 digits. */
  private readonly money = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 2 });
  price(amount: number) {
    // Intl uses a narrow no-break space (U+202F) that some fonts draw with no width.
    return `${this.money.format(amount).replace(/ /g, ' ')} MAD`;
  }

  canCancel(card: ReservationCard) {
    return card.tab === 'attente' || card.tab === 'confirmees';
  }
  canReview(card: ReservationCard) {
    return card.tab === 'historique' && card.row.statut === 'ACCEPTEE';
  }
  setSearch(event: Event) {
    this.search.set((event.target as HTMLInputElement).value);
  }
  reload() {
    this.refresh.next();
  }
  cancel(card: ReservationCard) {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.message.set('');
    this.api
      .decision(card.row.id, 'annuler')
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: () => {
          this.pending.set(null);
          this.message.set(`La réservation n° ${card.row.id} a été annulée.`);
          this.reload();
        },
        error: (e) => this.error.set(apiError(e)),
      });
  }
}
