import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize, forkJoin, of } from 'rxjs';
import { Guide } from '../../models/circuit';
import { CircuitService } from '../../services/circuit.service';
import { GuideService } from '../../services/guide.service';
import { apiError } from '../../services/workspace.service';
@Component({
  selector: 'app-circuit-editor',
  imports: [ReactiveFormsModule, RouterLink],
  host: { class: 'workspace-ui' },
  template: ` <header class="section-header">
      <div>
        <h2>{{ id === null ? 'Créer un circuit' : 'Modifier le circuit' }}</h2>
        <p>Décrivez la visite et choisissez son guide.</p>
      </div>
    </header>
    @if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
    }
    @if (loading()) {
      <p class="surface" role="status">Chargement…</p>
    } @else if (ready()) {
      <form class="surface" [formGroup]="form" (ngSubmit)="save()">
        <h3 class="panel-heading">Informations du circuit</h3>
        <div class="form-grid">
          <div class="full">
            <label for="c-title">Titre</label
            ><input id="c-title" formControlName="titre" maxlength="160" />
          </div>
          <div class="full">
            <label for="c-description">Description</label
            ><textarea id="c-description" formControlName="description" maxlength="4000"></textarea>
          </div>
          <div>
            <label for="c-city">Ville</label
            ><input id="c-city" formControlName="ville" maxlength="120" />
          </div>
          <div>
            <label for="c-duration">Durée en heures</label
            ><input id="c-duration" type="number" min="1" step="1" formControlName="dureeHeures" />
          </div>
          <div>
            <label for="c-price">Prix par personne (MAD)</label
            ><input id="c-price" type="number" min="0" step="0.01" formControlName="prix" />
          </div>
          <div>
            <label for="c-guide">Guide associé</label
            ><select id="c-guide" formControlName="guideId">
              <option [ngValue]="0" disabled>Choisir un guide</option>
              @for (g of guides(); track g.id) {
                <option [ngValue]="g.id">{{ g.nom }} · {{ g.ville }}</option>
              }
            </select>
          </div>
        </div>
        @if (submitted() && form.invalid) {
          <p class="form-errors" role="alert">
            Renseignez tous les champs. La durée doit être un nombre entier positif, le prix positif
            ou nul avec deux décimales maximum.
          </p>
        }
        <div class="actions">
          <button class="button primary" [disabled]="saving()">
            {{ saving() ? 'Enregistrement…' : 'Enregistrer le circuit' }}</button
          ><a class="button secondary" routerLink="/administration/circuits">Annuler</a>
        </div>
      </form>
    }`,
})
export class CircuitEditor {
  private readonly api = inject(CircuitService);
  private readonly guideApi = inject(GuideService);
  private readonly router = inject(Router);
  private readonly param = inject(ActivatedRoute).snapshot.paramMap.get('id');
  readonly id = this.param === null ? null : Number(this.param);
  readonly guides = signal<Guide[]>([]);
  readonly error = signal('');
  readonly loading = signal(true);
  readonly ready = signal(false);
  readonly saving = signal(false);
  readonly submitted = signal(false);
  readonly form = inject(FormBuilder).nonNullable.group({
    titre: ['', [Validators.required, Validators.maxLength(160)]],
    description: ['', [Validators.required, Validators.maxLength(4000)]],
    ville: ['', [Validators.required, Validators.maxLength(120)]],
    dureeHeures: [1, [Validators.min(1), Validators.pattern(/^\d+$/)]],
    prix: [
      0,
      [Validators.min(0), Validators.max(99999999.99), Validators.pattern(/^\d+(\.\d{1,2})?$/)],
    ],
    guideId: [0, Validators.min(1)],
  });
  constructor() {
    if (this.id !== null && (!Number.isSafeInteger(this.id) || this.id < 1)) {
      this.loading.set(false);
      this.error.set('Circuit invalide.');
      return;
    }
    forkJoin({
      guides: this.guideApi.list(),
      circuit: this.id === null ? of(null) : this.api.get(this.id),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ guides, circuit }) => {
          this.guides.set(guides);
          if (circuit)
            this.form.patchValue({
              ...circuit,
              description: circuit.description || '',
              ville: circuit.ville || '',
              dureeHeures: circuit.dureeHeures || 1,
            });
          this.ready.set(true);
        },
        error: (e) => this.error.set(apiError(e)),
      });
  }
  save() {
    this.submitted.set(true);
    if (this.form.invalid || this.saving()) return;
    this.saving.set(true);
    this.error.set('');
    this.api
      .save(this.id, this.form.getRawValue())
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => void this.router.navigate(['/administration/circuits']),
        error: (e) => this.error.set(apiError(e)),
      });
  }
}
