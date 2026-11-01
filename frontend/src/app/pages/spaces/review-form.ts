import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { Eligibility, WorkspaceService, apiError } from '../../services/workspace.service';
@Component({
  selector: 'app-review-form',
  imports: [RouterLink, ReactiveFormsModule],
  host: { class: 'workspace-ui' },
  template: ` <header class="section-header">
      <div>
        <h2>Racontez votre visite</h2>
        <p>Votre avis aide les prochains voyageurs à choisir.</p>
      </div>
    </header>
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
        <a class="button primary" routerLink="/espace-touriste/avis">Voir mes avis</a>
      </section>
    } @else if (eligibility(); as e) {
      @if (!e.eligible) {
        <p class="notice">{{ e.raison }}</p>
        <a class="button secondary" routerLink="/espace-touriste">Retour aux réservations</a>
      } @else {
        <form class="surface" [formGroup]="form" (ngSubmit)="save()">
          <h3 class="panel-heading">Réservation n° {{ e.reservationId }}</h3>
          <div class="form-grid" style="margin-top:22px">
            <div>
              <label for="note-guide">Votre guide</label
              ><select id="note-guide" formControlName="noteGuide">
                @for (n of notes; track n) {
                  <option [ngValue]="n">{{ n }} / 5</option>
                }
              </select>
            </div>
            <div>
              <label for="note-circuit">Votre circuit</label
              ><select id="note-circuit" formControlName="noteCircuit">
                @for (n of notes; track n) {
                  <option [ngValue]="n">{{ n }} / 5</option>
                }
              </select>
            </div>
            <div class="full">
              <label for="review-comment">Votre commentaire</label
              ><textarea
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
            <button class="button primary" [disabled]="saving()">
              {{ saving() ? 'Publication…' : 'Publier mon avis' }}</button
            ><a class="button secondary" routerLink="/espace-touriste">Retour</a>
          </div>
        </form>
      }
    }`,
})
export class ReviewForm {
  private readonly api = inject(WorkspaceService);
  private readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  readonly notes = [5, 4, 3, 2, 1];
  readonly eligibility = signal<Eligibility | null>(null);
  readonly loading = signal(true);
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
    if (!Number.isSafeInteger(this.id) || this.id < 1) {
      this.loading.set(false);
      this.error.set('Réservation invalide.');
      return;
    }
    this.api
      .eligibility(this.id)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (e) => this.eligibility.set(e),
        error: (e) => this.error.set(apiError(e)),
      });
  }
  save() {
    this.submitted.set(true);
    if (this.form.invalid || this.saving() || !this.eligibility()?.eligible) return;
    this.saving.set(true);
    this.error.set('');
    this.api
      .createAvis({ ...this.form.getRawValue(), reservationId: this.id })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({ next: () => this.success.set(true), error: (e) => this.error.set(apiError(e)) });
  }
}
