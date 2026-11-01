import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { WorkspaceService, apiError } from '../../services/workspace.service';
@Component({
  selector: 'app-guide-editor',
  imports: [ReactiveFormsModule, RouterLink],
  host: { class: 'workspace-ui' },
  template: ` <header class="section-header">
      <div>
        <h2>{{ id === null ? 'Créer un guide' : 'Modifier le guide' }}</h2>
        <p>Le profil touristique et son compte de connexion restent séparés.</p>
      </div>
    </header>
    @if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
    }
    @if (loading()) {
      <p class="surface" role="status">Chargement…</p>
    } @else if (ready()) {
      <form class="surface" [formGroup]="form" (ngSubmit)="save()">
        <h3 class="panel-heading">Informations du guide</h3>
        <div class="form-grid">
          <div>
            <label for="g-name">Nom complet</label
            ><input id="g-name" formControlName="nom" maxlength="160" />
          </div>
          <div>
            <label for="g-city">Ville</label
            ><input id="g-city" formControlName="ville" maxlength="120" />
          </div>
          <div class="full">
            <label for="g-languages">Langues (séparées par une virgule)</label
            ><input id="g-languages" formControlName="langues" />
          </div>
          <div>
            <label for="g-price">Tarif journalier (MAD)</label
            ><input id="g-price" type="number" min="0" step="0.01" formControlName="tarifParJour" />
          </div>
          <div>
            <label for="g-email">E-mail</label
            ><input id="g-email" type="email" formControlName="email" maxlength="160" />
          </div>
          <div>
            <label for="g-phone">Téléphone</label
            ><input id="g-phone" type="tel" formControlName="telephone" maxlength="40" />
          </div>
          <div>
            <label for="g-account">Identifiant du compte Guide (UUID)</label
            ><input id="g-account" formControlName="ownerSubject" />
            <p class="hint">
              Facultatif. Copiez l’identifiant de l’utilisateur Keycloak ayant le rôle GUIDE. Le nom
              du compte ne suffit pas.
            </p>
          </div>
          <label class="check-label full"
            ><input type="checkbox" formControlName="disponible" />Disponible pour de nouvelles
            réservations</label
          >
        </div>
        @if (submitted() && form.invalid) {
          <p class="form-errors" role="alert">
            Vérifiez les champs obligatoires, le tarif et l’identifiant UUID.
          </p>
        }
        <div class="actions">
          <button class="button primary" [disabled]="saving()">
            {{ saving() ? 'Enregistrement…' : 'Enregistrer le guide' }}</button
          ><a class="button secondary" routerLink="/administration/guides">Annuler</a>
        </div>
      </form>
    }`,
})
export class GuideEditor {
  private readonly api = inject(WorkspaceService);
  private readonly router = inject(Router);
  private readonly param = inject(ActivatedRoute).snapshot.paramMap.get('id');
  readonly id = this.param === null ? null : Number(this.param);
  readonly loading = signal(false);
  readonly ready = signal(this.id === null);
  readonly saving = signal(false);
  readonly submitted = signal(false);
  readonly error = signal('');
  readonly form = inject(FormBuilder).nonNullable.group({
    nom: ['', [Validators.required, Validators.maxLength(160)]],
    ville: ['', [Validators.required, Validators.maxLength(120)]],
    langues: ['', Validators.required],
    tarifParJour: [0, [Validators.min(0), Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
    email: ['', Validators.email],
    telephone: '',
    ownerSubject: ['', Validators.pattern(/^[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$/)],
    disponible: true,
  });
  constructor() {
    if (this.id !== null) {
      this.loading.set(true);
      this.api
        .adminGuides()
        .pipe(finalize(() => this.loading.set(false)))
        .subscribe({
          next: (guides) => {
            const guide = guides.find((g) => g.id === this.id);
            if (!guide) {
              this.error.set('Guide introuvable.');
              return;
            }
            this.form.patchValue({
              ...guide,
              langues: guide.langues?.join(', ') || '',
              email: guide.email || '',
              telephone: guide.telephone || '',
              ownerSubject: guide.ownerSubject || '',
            });
            this.ready.set(true);
          },
          error: (e) => this.error.set(apiError(e)),
        });
    }
  }
  save() {
    this.submitted.set(true);
    if (this.form.invalid || this.saving()) return;
    const raw = this.form.getRawValue();
    const langues = raw.langues
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);
    if (!langues.length) {
      this.error.set('Indiquez au moins une langue.');
      return;
    }
    this.saving.set(true);
    this.api
      .saveGuide(this.id, { ...raw, langues, ownerSubject: raw.ownerSubject || null })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => void this.router.navigate(['/administration/guides']),
        error: (e) => this.error.set(apiError(e)),
      });
  }
}
