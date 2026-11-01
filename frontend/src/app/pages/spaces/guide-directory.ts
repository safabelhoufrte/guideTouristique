import { CurrencyPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { Subject, startWith, switchMap, map, catchError, of } from 'rxjs';
import { Guide } from '../../models/circuit';
import { GuideService } from '../../services/guide.service';
import { apiError } from '../../services/workspace.service';
@Component({
  selector: 'app-guide-directory',
  imports: [CurrencyPipe, ReactiveFormsModule, RouterLink],
  host: { class: 'workspace-ui' },
  template: ` <section class="workspace-banner">
      <div class="workspace">
        <h1>Nos guides</h1>
        <p>Des guides locaux pour découvrir une ville autrement.</p>
      </div>
    </section>
    <main class="workspace" style="padding-top:28px;padding-bottom:56px">
      <form class="surface form-grid" [formGroup]="form" (ngSubmit)="refresh.next()">
        <div>
          <label for="guide-city">Ville</label
          ><input id="guide-city" formControlName="ville" placeholder="Ex. Agadir" />
        </div>
        <div>
          <label for="guide-language">Langue</label
          ><input id="guide-language" formControlName="langue" placeholder="Ex. Français" />
        </div>
        <div class="full"><button class="button primary">Filtrer</button></div>
      </form>
      @if (vm().loading) {
        <p role="status">Chargement…</p>
      } @else if (vm().error) {
        <p class="error" role="alert">{{ vm().error }}</p>
      } @else if (!vm().rows.length) {
        <p class="surface empty">Aucun guide pour cette recherche.</p>
      } @else {
        <div class="directory-grid">
          @for (g of vm().rows; track g.id) {
            <article class="surface">
              <h2>{{ g.nom }}</h2>
              <p class="muted">{{ g.ville }}</p>
              <p>{{ g.langues?.join(', ') }}</p>
              <span class="badge">{{ g.disponible ? 'Disponible' : 'Indisponible' }}</span>
              <p>
                <strong>{{ g.tarifParJour | currency: 'MAD' : 'code' }}</strong> / jour
              </p>
              <a class="button primary" routerLink="/circuits" [queryParams]="{ guideId: g.id }"
                >Voir ses circuits</a
              >
            </article>
          }
        </div>
      }
    </main>`,
})
export class GuideDirectory {
  private readonly api = inject(GuideService);
  readonly refresh = new Subject<void>();
  readonly form = inject(FormBuilder).nonNullable.group({ ville: '', langue: '' });
  readonly vm = toSignal(
    this.refresh.pipe(
      startWith(undefined),
      switchMap(() =>
        this.api.list(this.form.getRawValue()).pipe(
          map((rows) => ({ loading: false, rows, error: '' })),
          catchError((e) => of({ loading: false, rows: [] as Guide[], error: apiError(e) })),
          startWith({ loading: true, rows: [] as Guide[], error: '' }),
        ),
      ),
    ),
    { initialValue: { loading: true, rows: [] as Guide[], error: '' } },
  );
}
