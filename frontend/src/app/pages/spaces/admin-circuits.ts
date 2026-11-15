import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, finalize, forkJoin, map, of, startWith, Subject, switchMap } from 'rxjs';
import { Circuit, Guide } from '../../models/circuit';
import { circuitPhoto } from '../../models/circuit-photo';
import { CircuitService } from '../../services/circuit.service';
import { GuideService } from '../../services/guide.service';
import { apiError } from '../../services/workspace.service';

interface AdminCatalogue {
  loading: boolean;
  rows: Circuit[];
  guides: Guide[];
  error: string;
}
const loadingState: AdminCatalogue = { loading: true, rows: [], guides: [], error: '' };

@Component({
  selector: 'app-admin-circuits',
  imports: [RouterLink],
  host: { class: 'workspace-ui' },
  templateUrl: './admin-circuits.html',
  styleUrl: './admin-circuits.scss',
})
export class AdminCircuits {
  private readonly api = inject(CircuitService);
  private readonly guidesApi = inject(GuideService);
  readonly refresh = new Subject<void>();
  readonly pending = signal<Circuit | null>(null);
  readonly error = signal('');
  readonly busy = signal(false);
  readonly search = signal('');
  readonly ville = signal('');
  readonly guideId = signal(0);
  readonly view = signal<'cards' | 'list'>('cards');
  readonly photoFor = circuitPhoto;

  /** Circuits are required; guide names are optional (fallback "Guide n° X"). */
  readonly vm = toSignal(
    this.refresh.pipe(
      startWith(undefined),
      switchMap(() =>
        forkJoin({
          rows: this.api.list(),
          guides: this.guidesApi.list().pipe(catchError(() => of([] as Guide[]))),
        }).pipe(
          map(({ rows, guides }): AdminCatalogue => ({ loading: false, rows, guides, error: '' })),
          catchError((e) => of<AdminCatalogue>({ ...loadingState, loading: false, error: apiError(e) })),
          startWith(loadingState),
        ),
      ),
    ),
    { initialValue: loadingState },
  );

  readonly stats = computed(() => {
    const rows = this.vm().rows;
    const total = rows.reduce((sum, c) => sum + c.prix, 0);
    return {
      count: rows.length,
      cities: new Set(rows.map((c) => c.ville).filter(Boolean)).size,
      guides: new Set(rows.map((c) => c.guideId)).size,
      average: rows.length ? Math.round(total / rows.length) : 0,
    };
  });
  readonly cities = computed(() =>
    [...new Set(this.vm().rows.map((c) => c.ville).filter((v): v is string => !!v))].sort(),
  );
  readonly guideOptions = computed(() => {
    const ids = [...new Set(this.vm().rows.map((c) => c.guideId))];
    return ids
      .map((id) => ({ id, nom: this.guideName(id) }))
      .sort((a, b) => a.nom.localeCompare(b.nom, 'fr'));
  });
  readonly visible = computed(() => {
    const text = this.search().trim().toLocaleLowerCase('fr');
    const ville = this.ville();
    const guideId = this.guideId();
    return this.vm().rows.filter(
      (c) =>
        (!ville || c.ville === ville) &&
        (!guideId || c.guideId === guideId) &&
        (!text ||
          `${c.titre} ${c.ville ?? ''} ${this.guideName(c.guideId)}`
            .toLocaleLowerCase('fr')
            .includes(text)),
    );
  });

  private readonly money = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 2 });
  price(amount: number) {
    return this.money.format(amount).replace(/ /g, ' ') + ' MAD';
  }
  guideName(id: number) {
    return this.vm().guides.find((g) => g.id === id)?.nom ?? `Guide n° ${id}`;
  }
  guideCity(id: number) {
    return this.vm().guides.find((g) => g.id === id)?.ville ?? '';
  }
  initials(name: string) {
    return name
      .split(/\s+/)
      .filter((part) => part && part !== 'n°')
      .slice(0, 2)
      .map((part) => part[0].toUpperCase())
      .join('');
  }
  value(event: Event) {
    return (event.target as HTMLInputElement | HTMLSelectElement).value;
  }

  archive(c: Circuit) {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.api
      .archive(c.id)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: () => {
          this.pending.set(null);
          this.refresh.next();
        },
        error: (e) => this.error.set(apiError(e)),
      });
  }
}
