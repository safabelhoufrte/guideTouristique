import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, map, of, startWith, Subject, switchMap, finalize } from 'rxjs';
import { Avis, WorkspaceService, apiError } from '../../services/workspace.service';
@Component({
  selector: 'app-reviews',
  imports: [DatePipe, RouterLink],
  host: { class: 'workspace-ui' },
  template: `<div
    [class.workspace]="scope === 'public'"
    [style.padding-top.px]="scope === 'public' ? 32 : null"
  >
    <header class="section-header">
      <div>
        <h2>
          {{
            scope === 'admin'
              ? 'Modérer les avis'
              : scope === 'me'
                ? 'Mes avis'
                : 'Avis des voyageurs'
          }}
        </h2>
        <p>
          {{
            scope === 'admin'
              ? 'Publiez ou masquez les contributions.'
              : 'Des souvenirs et des retours sur les visites.'
          }}
        </p>
      </div>
      <button class="button secondary" (click)="refresh.next()">Actualiser</button>
    </header>
    @if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
    }
    @if (vm().loading) {
      <p class="surface" role="status">Chargement des avis…</p>
    } @else if (vm().error) {
      <p class="error" role="alert">{{ vm().error }}</p>
    } @else if (!vm().rows.length) {
      <section class="surface empty">
        <h3>Aucun avis pour le moment</h3>
        <p>Les avis sont possibles après une visite acceptée.</p>
        @if (scope === 'me') {
          <a class="button primary" routerLink="/espace-touriste">Voir mes réservations</a>
        }
      </section>
    } @else {
      <div class="stack">
        @for (a of vm().rows; track a.id) {
          <article class="surface">
            <div class="booking-heading">
              <h3>{{ a.auteur }}</h3>
              <span class="badge">{{ a.publie ? 'Publié' : 'Masqué' }}</span>
            </div>
            <p class="review-notes">
              Guide : {{ a.noteGuide }}/5 · Circuit : {{ a.noteCircuit }}/5
            </p>
            <p class="review-text">{{ a.commentaire }}</p>
            <p class="muted">
              Circuit n° {{ a.circuitId }} · {{ a.dateCreation | date: 'dd/MM/yyyy' }}
            </p>
            @if (scope === 'admin') {
              <div class="actions">
                <button class="button secondary" [disabled]="busy()" (click)="toggle(a)">
                  {{ a.publie ? 'Masquer cet avis' : 'Publier cet avis' }}
                </button>
              </div>
            }
          </article>
        }
      </div>
    }
  </div>`,
})
export class Reviews {
  private readonly api = inject(WorkspaceService);
  readonly scope = (inject(ActivatedRoute).snapshot.data['scope'] || 'public') as
    'public' | 'me' | 'admin';
  readonly refresh = new Subject<void>();
  readonly error = signal('');
  readonly busy = signal(false);
  readonly vm = toSignal(
    this.refresh.pipe(
      startWith(undefined),
      switchMap(() =>
        this.api.avis(this.scope).pipe(
          map((rows) => ({ loading: false, rows, error: '' })),
          catchError((e) => of({ loading: false, rows: [] as Avis[], error: apiError(e) })),
          startWith({ loading: true, rows: [] as Avis[], error: '' }),
        ),
      ),
    ),
    { initialValue: { loading: true, rows: [] as Avis[], error: '' } },
  );
  toggle(a: Avis) {
    if (this.busy()) return;
    this.busy.set(true);
    this.api
      .publish(a.id, !a.publie)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({ next: () => this.refresh.next(), error: (e) => this.error.set(apiError(e)) });
  }
}
