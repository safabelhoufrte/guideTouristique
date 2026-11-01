import { CurrencyPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, map, of, startWith, Subject, switchMap } from 'rxjs';
import { Circuit } from '../../models/circuit';
import { WorkspaceService, apiError } from '../../services/workspace.service';
@Component({
  selector: 'app-guide-circuits',
  imports: [RouterLink, CurrencyPipe],
  host: { class: 'workspace-ui' },
  template: ` <header class="section-header">
      <div>
        <h2>Mes circuits</h2>
        <p>Les visites qui vous sont confiées par l’administrateur.</p>
      </div>
      <button class="button secondary" (click)="refresh.next()">Actualiser</button>
    </header>
    @if (vm().loading) {
      <p class="surface" role="status">Chargement…</p>
    } @else if (vm().error) {
      <p class="error" role="alert">{{ vm().error }}</p>
    } @else if (!vm().rows.length) {
      <section class="surface empty">
        <h3>Aucun circuit associé</h3>
        <p>Contactez l’administrateur pour organiser vos prochaines visites.</p>
      </section>
    } @else {
      <div class="assigned-grid">
        @for (c of vm().rows; track c.id) {
          <article class="surface assigned-card">
            <img
              class="assigned-cover"
              src="/images/circuit-landscape.svg"
              alt=""
              loading="lazy"
              width="600"
              height="170"
            />
            <h3>{{ c.titre }}</h3>
            <p class="muted">{{ c.ville }} · {{ c.dureeHeures }} h</p>
            <p class="assigned-description">{{ c.description }}</p>
            <p class="price-line">
              <strong>{{ c.prix | currency: 'MAD' : 'code' }}</strong> / personne
            </p>
            <a class="button secondary" [routerLink]="['/circuits', c.id]"
              >Voir la fiche publique</a
            >
          </article>
        }
      </div>
    }`,
})
export class GuideCircuits {
  private readonly api = inject(WorkspaceService);
  readonly refresh = new Subject<void>();
  readonly vm = toSignal(
    this.refresh.pipe(
      startWith(undefined),
      switchMap(() =>
        this.api.assignedCircuits().pipe(
          map((rows) => ({ loading: false, rows, error: '' })),
          catchError((e) => of({ loading: false, rows: [] as Circuit[], error: apiError(e) })),
          startWith({ loading: true, rows: [] as Circuit[], error: '' }),
        ),
      ),
    ),
    { initialValue: { loading: true, rows: [] as Circuit[], error: '' } },
  );
}
