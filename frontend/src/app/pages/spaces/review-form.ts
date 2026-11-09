import { Component, EventEmitter, inject, Input, Output, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { Avis, Eligibility, extractAvisError } from '../../models/avis';
import { AvisService } from '../../services/avis.service';

@Component({
  selector: 'app-review-form',
  imports: [RouterLink, ReactiveFormsModule],
  host: { class: 'workspace-ui' },
  styles: [
    `
      .btn-publish {
        display: inline-flex;
        justify-content: center;
        align-items: center;
        min-height: 44px;
        padding: 14px 28px;
        border: 1px solid transparent;
        border-radius: 10px;
        font: inherit;
        font-size: 16px;
        font-weight: 600;
        color: #ffffff;
        background: #ea580c;
        cursor: pointer;
        transition: background-color 0.18s;
        text-decoration: none;
      }
      .btn-publish:hover:not(:disabled) {
        background: #c2410c;
      }
      .btn-publish:disabled {
        opacity: 0.55;
        cursor: not-allowed;
      }
      .sub-heading {
        margin: 0 0 16px;
        font-size: 18px;
        color: var(--primary, #0e7490);
        font-weight: 600;
      }
    `,
  ],
  template: `
    @if (!embedded) {
      <header class="section-header">
        <div>
          <h2>Racontez votre visite</h2>
          <p>Votre avis aide les prochains voyageurs à choisir.</p>
        </div>
      </header>
    } @else {
      <h3 class="panel-heading">Donner mon avis</h3>
    }
    @if (loading()) {
      <p class="surface" role="status">Vérification de votre réservation…</p>
    }
    @if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
    }
    @if (success()) {
      <section class="surface empty">
        <h3>Merci pour votre avis !</h3>
        <p>Votre contribution a bien été enregistrée.</p>
        @if (!embedded) {
          <a class="button primary" routerLink="/espace-touriste/avis">Voir mes avis</a>
        }
      </section>
    } @else if (eligibility(); as e) {
      @if (!e.eligible) {
        <p class="notice">{{ e.raison }}</p>
        @if (!embedded) {
          <a class="button secondary" routerLink="/espace-touriste">Retour aux réservations</a>
        }
      } @else {
        <form class="surface" [formGroup]="form" (ngSubmit)="save()">
          <h4 class="sub-heading">Réservation n° {{ e.reservationId }}</h4>
          <div class="form-grid" style="margin-top:22px">
            <div>
              <label for="note-guide">Votre guide</label>
              <select id="note-guide" formControlName="noteGuide" aria-label="Note du guide">
                @for (n of notes; track n) {
                  <option [ngValue]="n">{{ n }} ★ / 5</option>
                }
              </select>
            </div>
            <div>
              <label for="note-circuit">Votre circuit</label>
              <select id="note-circuit" formControlName="noteCircuit" aria-label="Note du circuit">
                @for (n of notes; track n) {
                  <option [ngValue]="n">{{ n }} ★ / 5</option>
                }
              </select>
            </div>
            <div class="full">
              <label for="review-comment">Votre commentaire</label>
              <textarea
                id="review-comment"
                formControlName="commentaire"
                maxlength="2000"
                placeholder="Qu’avez-vous aimé pendant cette visite ?"
              ></textarea>
            </div>
          </div>
          @if (submitted() && form.invalid) {
            <p class="form-errors" role="alert">
              Ajoutez un commentaire et deux notes entre 1 et 5.
            </p>
          }
          <div class="actions">
            <button class="btn-publish" type="submit" [disabled]="saving()">
              {{ saving() ? 'Publication…' : 'Publier' }}
            </button>
            @if (!embedded) {
              <a class="button secondary" routerLink="/espace-touriste">Retour</a>
            }
          </div>
        </form>
      }
    }
  `,
})
export class ReviewForm {
  private readonly api = inject(AvisService);
  private readonly route = inject(ActivatedRoute);

  @Input() set reservationId(val: number | null | undefined) {
    if (val !== undefined && val !== null) {
      const num = Number(val);
      this.currentId.set(num);
      this.loadEligibility(num);
    }
  }

  @Input() embedded = false;
  @Output() readonly reviewCreated = new EventEmitter<Avis>();

  readonly currentId = signal<number | null>(null);
  readonly notes = [5, 4, 3, 2, 1];
  readonly eligibility = signal<Eligibility | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly success = signal(false);
  readonly submitted = signal(false);

  readonly form = inject(FormBuilder).nonNullable.group({
    noteGuide: [5, [Validators.min(1), Validators.max(5)]],
    noteCircuit: [5, [Validators.min(1), Validators.max(5)]],
    commentaire: ['', [Validators.required, Validators.maxLength(2000)]],
  });

  constructor() {
    const rawId = this.route.snapshot.paramMap.get('id');
    if (rawId !== null) {
      const routeId = Number(rawId);
      if (!Number.isSafeInteger(routeId) || routeId < 1) {
        this.error.set('Réservation invalide.');
        return;
      }
      this.currentId.set(routeId);
      this.loadEligibility(routeId);
    }
  }

  loadEligibility(id: number) {
    if (!Number.isSafeInteger(id) || id < 1) {
      this.error.set('Réservation invalide.');
      return;
    }
    this.loading.set(true);
    this.error.set('');
    this.api
      .checkEligibility(id)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (e) => this.eligibility.set(e),
        error: (e) => this.error.set(extractAvisError(e)),
      });
  }

  save() {
    this.submitted.set(true);
    const resId = this.currentId();
    if (this.form.invalid || this.saving() || !this.eligibility()?.eligible || !resId) {
      return;
    }
    this.saving.set(true);
    this.error.set('');
    this.api
      .createAvis({ ...this.form.getRawValue(), reservationId: resId })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (created) => {
          this.success.set(true);
          this.reviewCreated.emit(created);
        },
        error: (e) => this.error.set(extractAvisError(e)),
      });
  }
}
