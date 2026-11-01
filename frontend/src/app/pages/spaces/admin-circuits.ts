import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, map, of, startWith, Subject, switchMap, finalize } from 'rxjs';
import { Circuit } from '../../models/circuit';
import { CircuitService } from '../../services/circuit.service';
import { apiError } from '../../services/workspace.service';
@Component({
  selector: 'app-admin-circuits',
  imports: [RouterLink, CurrencyPipe],
  host: { class: 'workspace-ui' },
  template: ` <header class="section-header">
      <div>
        <h2>Gérer les circuits</h2>
        <p>Un guide unique est associé à chaque circuit.</p>
      </div>
      <a class="button primary" routerLink="nouveau">Créer un circuit</a>
    </header>
    @if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
    }
    @if (pending(); as c) {
      <section class="confirm-box">
        <h3>Archiver « {{ c.titre }} » ?</h3>
        <p>Le circuit sera retiré du catalogue. Les anciennes réservations seront conservées.</p>
        <div class="actions">
          <button class="button danger" [disabled]="busy()" (click)="archive(c)">Archiver</button
          ><button class="button secondary" [disabled]="busy()" (click)="pending.set(null)">
            Conserver
          </button>
        </div>
      </section>
    }
    @if (vm().loading) {
      <p class="surface" role="status">Chargement…</p>
    } @else if (vm().error) {
      <p class="error" role="alert">{{ vm().error }}</p>
      <button class="button secondary" (click)="refresh.next()">Réessayer</button>
    } @else if (!vm().rows.length) {
      <section class="surface empty">
        <h3>Votre catalogue est vide</h3>
        <a class="button primary" routerLink="nouveau">Créer le premier circuit</a>
      </section>
    } @else {
      <div class="surface table-wrap">
        <table>
          <thead>
            <tr>
              <th>Circuit</th>
              <th>Guide</th>
              <th>Prix</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            @for (c of vm().rows; track c.id) {
              <tr>
                <td>
                  <strong>{{ c.titre }}</strong
                  ><small>{{ c.ville }} · {{ c.dureeHeures }} h</small>
                </td>
                <td>N° {{ c.guideId }}</td>
                <td>{{ c.prix | currency: 'MAD' : 'code' }}</td>
                <td>
                  <div class="table-actions">
                    <a class="button secondary" [routerLink]="[c.id]">Modifier</a
                    ><button class="button danger" [disabled]="busy()" (click)="pending.set(c)">
                      Archiver
                    </button>
                  </div>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }`,
})
export class AdminCircuits {
  private readonly api = inject(CircuitService);
  readonly refresh = new Subject<void>();
  readonly pending = signal<Circuit | null>(null);
  readonly error = signal('');
  readonly busy = signal(false);
  readonly vm = toSignal(
    this.refresh.pipe(
      startWith(undefined),
      switchMap(() =>
        this.api.list().pipe(
          map((rows) => ({ loading: false, rows, error: '' })),
          catchError((e) => of({ loading: false, rows: [] as Circuit[], error: apiError(e) })),
          startWith({ loading: true, rows: [] as Circuit[], error: '' }),
        ),
      ),
    ),
    { initialValue: { loading: true, rows: [] as Circuit[], error: '' } },
  );
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
