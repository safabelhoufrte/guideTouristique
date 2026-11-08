import { DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, map, of, startWith, Subject, switchMap } from 'rxjs';
import { Avis, WorkspaceService, apiError } from '../../services/workspace.service';
@Component({
  selector: 'app-reviews',
  imports: [DatePipe],
  host: { class: 'workspace-ui' },
  template: `<main class="workspace">
    <header class="section-header">
      <div>
        <h1>Avis des voyageurs</h1>
        <p>Des souvenirs et des retours sur les visites.</p>
      </div>
      <button class="button secondary" (click)="refresh.next()">Actualiser</button>
    </header>
    @if (vm().loading) {
      <p role="status">Chargement des avis…</p>
    } @else if (vm().error) {
      <p class="error" role="alert">{{ vm().error }}</p>
    } @else if (!vm().rows.length) {
      <section class="surface empty">
        <h2>Aucun avis pour le moment</h2>
        <p>Les avis sont possibles après une visite acceptée.</p>
      </section>
    } @else {
      <div class="stack">
        @for (a of vm().rows; track a.id) {
          <article class="surface">
            <h2>{{ a.auteur }}</h2>
            <p class="review-notes">
              Guide : {{ a.noteGuide }}/5 · Circuit : {{ a.noteCircuit }}/5
            </p>
            <p class="review-text">{{ a.commentaire }}</p>
            <p class="muted">{{ a.dateCreation | date: 'dd/MM/yyyy' }}</p>
          </article>
        }
      </div>
    }
  </main>`,
})
export class Reviews {
  private readonly api = inject(WorkspaceService);
  readonly refresh = new Subject<void>();
  readonly vm = toSignal(
    this.refresh.pipe(
      startWith(undefined),
      switchMap(() =>
        this.api.avis('public').pipe(
          map((rows) => ({ loading: false, rows: rows.filter((a) => a.publie), error: '' })),
          catchError((e) => of({ loading: false, rows: [] as Avis[], error: apiError(e) })),
          startWith({ loading: true, rows: [] as Avis[], error: '' }),
        ),
      ),
    ),
    { initialValue: { loading: true, rows: [] as Avis[], error: '' } },
  );
}
