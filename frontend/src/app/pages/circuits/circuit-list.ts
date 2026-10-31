import { CurrencyPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
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
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe],
  templateUrl: './circuit-list.html',
  styleUrl: './circuit-list.scss',
})
export class CircuitList {
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

  guideName(id: number) {
    return this.vm().guides.find((guide) => guide.id === id)?.nom ?? 'Guide associé';
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
    void this.router.navigate(['/circuits']);
  }

  retry() {
    this.refresh.next();
  }
}
