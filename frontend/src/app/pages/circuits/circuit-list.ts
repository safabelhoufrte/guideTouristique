import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  catchError,
  combineLatest,
  forkJoin,
  map,
  of,
  startWith,
  Subject,
  switchMap,
  tap,
} from 'rxjs';
import { Circuit, CircuitFilters, Guide } from '../../models/circuit';
import { CircuitService } from '../../services/circuit.service';
import { GuideService } from '../../services/guide.service';
import { circuitPhoto } from '../../models/circuit-photo';

interface Catalogue {
  loading: boolean;
  circuits: Circuit[];
  guides: Guide[];
  guideError: boolean;
  error: boolean;
}
const loading: Catalogue = {
  loading: true,
  circuits: [],
  guides: [],
  guideError: false,
  error: false,
};

@Component({
  selector: 'app-circuit-list',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './circuit-list.html',
  styleUrl: './circuit-list.scss',
})
export class CircuitList {
  readonly photoFor = circuitPhoto;
  private readonly api = inject(CircuitService);
  private readonly guides = inject(GuideService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly refresh = new Subject<void>();
  readonly form = inject(FormBuilder).nonNullable.group({ ville: '', guideId: '' });

  readonly vm = toSignal(
    combineLatest([this.route.queryParamMap, this.refresh.pipe(startWith(undefined))]).pipe(
      tap(([params]) =>
        this.form.patchValue({
          ville: params.get('ville') ?? '',
          guideId: params.get('guideId') ?? '',
        }),
      ),
      switchMap(([params]) => {
        const filters: CircuitFilters = { ville: params.get('ville') || undefined };
        const guideId = Number(params.get('guideId'));
        if (Number.isSafeInteger(guideId) && guideId > 0) filters.guideId = guideId;
        return forkJoin({
          circuits: this.api.list(filters),
          guides: this.guides.list().pipe(
            map((guides) => ({ guides, guideError: false })),
            catchError(() => of({ guides: [] as Guide[], guideError: true })),
          ),
        }).pipe(
          map(({ circuits, guides }): Catalogue => ({
            ...guides,
            circuits,
            loading: false,
            error: false,
          })),
          catchError(() => of<Catalogue>({ ...loading, loading: false, error: true })),
          startWith(loading),
        );
      }),
    ),
    { initialValue: loading },
  );

  /** Client-side refinements on top of the API filters (ville, guideId stay in the URL). */
  readonly duree = signal<'' | 'court' | 'moyen' | 'long'>('');
  readonly budget = signal(0);
  readonly sort = signal<'pertinence' | 'prix-asc' | 'prix-desc' | 'duree'>('pertinence');

  readonly visible = computed(() => {
    const duree = this.duree();
    const budget = this.budget();
    const rows = this.vm().circuits.filter((c) => {
      const h = c.dureeHeures;
      if (duree === 'court' && !(h !== null && h <= 4)) return false;
      if (duree === 'moyen' && !(h !== null && h > 4 && h <= 8)) return false;
      if (duree === 'long' && !(h !== null && h > 8)) return false;
      return !budget || c.prix <= budget;
    });
    const sort = this.sort();
    if (sort === 'prix-asc') return [...rows].sort((a, b) => a.prix - b.prix);
    if (sort === 'prix-desc') return [...rows].sort((a, b) => b.prix - a.prix);
    if (sort === 'duree') return [...rows].sort((a, b) => (a.dureeHeures ?? 1e9) - (b.dureeHeures ?? 1e9));
    return rows;
  });

  /** City chips: the cities present in the current results. */
  readonly cities = computed(() =>
    [...new Set(this.vm().circuits.map((c) => c.ville).filter((v): v is string => !!v))].sort(),
  );

  private readonly money = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 2 });
  price(amount: number) {
    return this.money.format(amount).replace(/ /g, ' ') + ' MAD';
  }

  guideName(id: number) {
    return this.vm().guides.find((guide) => guide.id === id)?.nom ?? 'Guide associé';
  }
  guideLangues(id: number) {
    return this.vm().guides.find((guide) => guide.id === id)?.langues?.join(', ') ?? '';
  }
  initials(name: string) {
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0].toUpperCase())
      .join('');
  }

  chooseCity(ville: string | null) {
    this.form.patchValue({ ville: ville ?? '' });
    this.filter();
  }
  setDuree(event: Event) {
    this.duree.set((event.target as HTMLSelectElement).value as '' | 'court' | 'moyen' | 'long');
  }
  setBudget(event: Event) {
    this.budget.set(Number((event.target as HTMLSelectElement).value) || 0);
  }
  setSort(event: Event) {
    this.sort.set(
      (event.target as HTMLSelectElement).value as 'pertinence' | 'prix-asc' | 'prix-desc' | 'duree',
    );
  }

  get filterParams() {
    return this.route.snapshot.queryParams;
  }

  filter() {
    const { ville, guideId } = this.form.getRawValue();
    const queryParams = { ville: ville.trim() || null, guideId: guideId || null };
    // A repeated submit is also a useful refresh after an outage.
    if (
      (this.route.snapshot.queryParamMap.get('ville') || null) === queryParams.ville &&
      (this.route.snapshot.queryParamMap.get('guideId') || null) === queryParams.guideId
    ) {
      this.retry();
    } else {
      void this.router.navigate(['/circuits'], { queryParams });
    }
  }

  reset() {
    this.form.reset();
    this.duree.set('');
    this.budget.set(0);
    void this.router.navigate(['/circuits']);
  }

  retry() {
    this.refresh.next();
  }
}
