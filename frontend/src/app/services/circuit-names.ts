import { inject, Injectable } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, map, of, shareReplay } from 'rxjs';
import { CircuitService } from './circuit.service';

/** Circuit titles by id, loaded once from the Circuit API (for pages that only know a circuitId). */
@Injectable({ providedIn: 'root' })
export class CircuitNames {
  private readonly names = toSignal(
    inject(CircuitService)
      .list()
      .pipe(
        map((circuits) => new Map(circuits.map((c) => [c.id, c.titre]))),
        catchError(() => of(new Map<number, string>())),
        shareReplay(1),
      ),
    { initialValue: new Map<number, string>() },
  );

  /** Circuit title, or "Circuit n° X" while loading or if the Circuit API is unavailable. */
  name(circuitId: number) {
    return this.names().get(circuitId) ?? `Circuit n° ${circuitId}`;
  }
}
