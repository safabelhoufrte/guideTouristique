import { CurrencyPipe, DecimalPipe, PercentPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, map, Observable, of, startWith, Subject, switchMap } from 'rxjs';
import { CircuitService } from '../../services/circuit.service';
import { apiError, WorkspaceService } from '../../services/workspace.service';
import { activityAnalytics, AnalyticsPeriod } from './admin-home.data';

function collection<T>(source: Observable<T[]>) {
  return source.pipe(
    map((rows) => ({ rows, error: '' })),
    catchError((error) => of({ rows: [] as T[], error: apiError(error) })),
  );
}

function businessDate(date: Date) {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Africa/Casablanca',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(date);
  return ['year', 'month', 'day']
    .map((type) => parts.find((part) => part.type === type)!.value)
    .join('-');
}

@Component({
  selector: 'app-admin-home',
  imports: [RouterLink, CurrencyPipe, DecimalPipe, PercentPipe],
  host: { class: 'workspace-ui' },
  templateUrl: './admin-home.html',
  styleUrl: './admin-home.scss',
})
export class AdminHome {
  private readonly api = inject(WorkspaceService);
  private readonly circuits = inject(CircuitService);
  private readonly refresh = new Subject<void>();
  readonly period = signal<AnalyticsPeriod>(90);
  readonly data = toSignal(
    this.refresh.pipe(
      startWith(undefined),
      switchMap(() =>
        forkJoin({
          reservations: collection(this.api.reservations('admin')),
          reviews: collection(this.api.avis('admin')),
          circuits: collection(this.circuits.list()),
          guides: collection(this.api.adminGuides()),
        }).pipe(
          map((data) => ({ ...data, updatedAt: new Date() })),
          startWith(null),
        ),
      ),
    ),
    { initialValue: null },
  );
  readonly report = computed(() => {
    const data = this.data();
    return data
      ? activityAnalytics(
          data.reservations.rows,
          data.reviews.rows,
          this.period(),
          businessDate(data.updatedAt),
        )
      : null;
  });
  readonly errors = computed(() => {
    const data = this.data();
    if (!data) return [];
    return [
      { name: 'Réservations', error: data.reservations.error },
      { name: 'Avis', error: data.reviews.error },
      { name: 'Circuits', error: data.circuits.error },
      { name: 'Guides', error: data.guides.error },
    ].filter((source) => source.error);
  });
  readonly statusSegments = computed(() => {
    const stats = this.report();
    let offset = 0;
    return (stats?.statuses ?? []).map((item) => {
      const share = stats?.bookings ? (item.count / stats.bookings) * 100 : 0;
      const segment = { ...item, share, offset };
      offset += share;
      return segment;
    });
  });
  setPeriod(event: Event) {
    const value = Number((event.target as HTMLSelectElement).value);
    if (value === 30 || value === 90 || value === 365) this.period.set(value);
  }
  circuitTitle(id: number) {
    return (
      this.data()?.circuits.rows.find((circuit) => circuit.id === id)?.titre ?? `Circuit n° ${id}`
    );
  }
  reload() {
    this.refresh.next();
  }
}
