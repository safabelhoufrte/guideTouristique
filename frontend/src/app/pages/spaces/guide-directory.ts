import { CurrencyPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  Subject,
  startWith,
  switchMap,
  map,
  catchError,
  of
} from 'rxjs';

import { Guide } from '../../models/circuit';
import { GuideService } from '../../services/guide.service';
import { apiError } from '../../services/workspace.service';

@Component({
  selector: 'app-guide-directory',

  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    RouterLink
  ],

  host: {
    class: 'workspace-ui'
  },

  template: `
    <section class="workspace-banner">
      <div class="workspace">
        <h1>Nos guides</h1>
        <p>
          Des guides locaux pour découvrir une ville autrement.
        </p>
      </div>
    </section>

    <main
      class="workspace"
      style="padding-top:28px;padding-bottom:56px"
    >

      <!-- Filtres -->
      <form
        class="surface form-grid"
        [formGroup]="form"
        (ngSubmit)="refresh.next()"
      >

        <div>
          <label for="guide-city">Ville</label>

          <input
            id="guide-city"
            formControlName="ville"
            placeholder="Ex. Agadir"
          />
        </div>

        <div>
          <label for="guide-language">Langue</label>

          <input
            id="guide-language"
            formControlName="langue"
            placeholder="Ex. Français"
          />
        </div>

        <div class="full">
          <button class="button primary" type="submit">
            Filtrer
          </button>
        </div>

      </form>

      <!-- Chargement -->
      @if (vm().loading) {

        <p role="status">
          Chargement des guides…
        </p>

      } @else if (vm().error) {

        <p class="error" role="alert">
          {{ vm().error }}
        </p>

      } @else if (!vm().rows.length) {

        <p class="surface empty">
          Aucun guide pour cette recherche.
        </p>

      } @else {

        <!-- Liste des guides -->
        <div class="directory-grid">

          @for (g of vm().rows; track g.id) {

            <article class="surface guide-card">

              <!-- Photo du guide -->
              <div class="guide-photo-container">

                @if (g.photo) {

                  <img
                    class="guide-photo"
                    [src]="g.photo"
                    [alt]="'Photo de ' + g.prenom + ' ' + g.nom"
                    loading="lazy"
                    width="96"
                    height="96"
                  />

                } @else {

                  <div
                    class="guide-photo-placeholder"
                    aria-label="Photo non disponible"
                  >
                    <span>👤</span>
                  </div>

                }

              </div>

              <!-- Nom du guide -->
              <h2 class="guide-name">
                {{ g.prenom }} {{ g.nom }}
              </h2>

              <!-- Ville -->
              <p class="muted guide-city">
                {{ g.ville }}
              </p>

              <!-- Langues -->
              <p class="guide-languages">
                {{ g.langues?.join(', ') }}
              </p>

              <!-- Disponibilité -->
              <span class="badge">
                {{
                  g.disponible
                    ? 'Disponible'
                    : 'Indisponible'
                }}
              </span>

              <!-- Tarif -->
              <p class="guide-price">
                <strong>
                  {{ g.tarifParJour | currency: 'MAD' : 'code' }}
                </strong>
                / jour
              </p>

              <!-- Circuits du guide -->
              <a
                class="button primary guide-button"
                routerLink="/circuits"
                [queryParams]="{ guideId: g.id }"
              >
                Voir ses circuits
              </a>

            </article>

          }

        </div>

      }

    </main>
  `,

  styles: [`
    /* Carte du guide */

    .guide-card {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      padding: 24px;
      border-radius: 12px;
      transition: transform 0.2s ease,
                  box-shadow 0.2s ease;
    }

    .guide-card:hover {
      transform: translateY(-3px);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
    }

    /* Photo */

    .guide-photo-container {
      display: flex;
      justify-content: center;
      width: 100%;
      margin-bottom: 18px;
    }

    .guide-photo {
      width: 96px;
      height: 96px;
      border-radius: 50%;
      object-fit: cover;
      object-position: center;
      display: block;
      border: 3px solid #edf4f6;
      background: #f1f5f9;
    }

    /* Photo par défaut */

    .guide-photo-placeholder {
      width: 96px;
      height: 96px;
      border-radius: 50%;
      background: #edf4f6;
      border: 3px solid #dce8ed;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 38px;
    }

    /* Informations */

    .guide-name {
      width: 100%;
      margin: 0 0 8px;
      text-align: center;
      font-size: 20px;
      font-weight: 700;
      color: #173b46;
    }

    .guide-city {
      width: 100%;
      text-align: center;
      margin: 0 0 10px;
    }

    .guide-languages {
      margin: 0 0 16px;
      font-size: 14px;
      color: #64748b;
      min-height: 20px;
    }

    .guide-price {
      margin: 20px 0;
      font-size: 15px;
      color: #334155;
    }

    .guide-price strong {
      color: #0d6679;
      font-size: 19px;
    }

    /* Bouton */

    .guide-button {
      margin-top: auto;
      text-align: center;
      width: 100%;
      justify-content: center;
    }

    /* Responsive */

    @media (max-width: 640px) {
      .guide-card {
        padding: 20px;
      }

      .guide-photo,
      .guide-photo-placeholder {
        width: 88px;
        height: 88px;
      }

      .guide-name {
        font-size: 18px;
      }
    }
  `]
})

export class GuideDirectory {

  private readonly api = inject(GuideService);

  readonly refresh = new Subject<void>();

  readonly form = inject(FormBuilder).nonNullable.group({
    ville: '',
    langue: ''
  });

  readonly vm = toSignal(
    this.refresh.pipe(

      startWith(undefined),

      switchMap(() =>
        this.api.list(this.form.getRawValue()).pipe(

          map((rows) => ({
            loading: false,
            rows,
            error: ''
          })),

          catchError((e) =>
            of({
              loading: false,
              rows: [] as Guide[],
              error: apiError(e)
            })
          ),

          startWith({
            loading: true,
            rows: [] as Guide[],
            error: ''
          })

        )
      )

    ),

    {
      initialValue: {
        loading: true,
        rows: [] as Guide[],
        error: ''
      }
    }
  );
}
