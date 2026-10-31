import { CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, combineLatest, map, of, startWith, Subject, switchMap } from 'rxjs';
import { Circuit, Guide } from '../../models/circuit';
import { CircuitService } from '../../services/circuit.service';
import { GuideService } from '../../services/guide.service';

interface Detail {
  loading: boolean;
  circuit: Circuit | null;
  guide: Guide | null;
  error: '' | 'missing' | 'unavailable';
}
const loading: Detail = { loading: true, circuit: null, guide: null, error: '' };

@Component({
  selector: 'app-circuit-detail',
  imports: [RouterLink, CurrencyPipe],
  templateUrl: './circuit-detail.html',
  styleUrl: './circuit-detail.scss',
})
export class CircuitDetail {
  private readonly api = inject(CircuitService);
  private readonly guides = inject(GuideService);
  private readonly route = inject(ActivatedRoute);
  private readonly refresh = new Subject<void>();
  readonly vm = toSignal(
    combineLatest([this.route.paramMap, this.refresh.pipe(startWith(undefined))]).pipe(
      switchMap(([params]) => {
        const id = Number(params.get('id'));
        if (!Number.isSafeInteger(id) || id < 1)
          return of<Detail>({ ...loading, loading: false, error: 'missing' });
        return this.api.get(id).pipe(
          switchMap((circuit) =>
            this.guides.get(circuit.guideId).pipe(
              map((guide): Detail => ({ loading: false, circuit, guide, error: '' })),
              catchError(() => of<Detail>({ loading: false, circuit, guide: null, error: '' })),
            ),
          ),
          catchError((error: HttpErrorResponse) =>
            of<Detail>({
              ...loading,
              loading: false,
              error: error.status === 404 ? 'missing' : 'unavailable',
            }),
          ),
          startWith(loading),
        );
      }),
    ),
    { initialValue: loading },
  );

  get filterParams() {
    const params = this.route.snapshot.queryParamMap;
    return { ville: params.get('ville'), guideId: params.get('guideId') };
  }

  retry() {
    this.refresh.next();
  }
}
