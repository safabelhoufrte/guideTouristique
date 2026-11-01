import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { GuideProfile, WorkspaceService, apiError } from '../../services/workspace.service';
@Component({
  selector: 'app-guide-profile',
  imports: [ReactiveFormsModule, CurrencyPipe],
  host: { class: 'workspace-ui' },
  template: `<header class="section-header">
      <div>
        <h2>Mon profil de guide</h2>
        <p>Vos informations publiques et votre disponibilité.</p>
      </div>
    </header>
    @if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
      <button class="button secondary" (click)="load()">Réessayer</button>
    }
    @if (message()) {
      <p class="success" role="status">{{ message() }}</p>
    }
    @if (loading()) {
      <p class="surface" role="status">Chargement du profil…</p>
    } @else if (profile(); as guide) {
      <form class="surface" [formGroup]="form" (ngSubmit)="save()">
        <h3 class="panel-heading">Mes informations</h3>
        <div class="form-grid">
          <div>
            <label for="profile-name">Nom affiché</label
            ><input id="profile-name" formControlName="nom" maxlength="160" />
          </div>
          <div>
            <label for="profile-city">Ville</label
            ><input id="profile-city" formControlName="ville" maxlength="120" />
          </div>
          <div class="full">
            <label for="profile-languages">Langues parlées</label
            ><input
              id="profile-languages"
              formControlName="langues"
              placeholder="Arabe, Français, Anglais"
            />
            <p class="hint">Séparez les langues par une virgule.</p>
          </div>
          <div>
            <label for="profile-email">E-mail de contact</label
            ><input id="profile-email" type="email" formControlName="email" maxlength="160" />
          </div>
          <div>
            <label for="profile-phone">Téléphone</label
            ><input id="profile-phone" type="tel" formControlName="telephone" maxlength="40" />
          </div>
          <label class="check-label full"
            ><input type="checkbox" formControlName="disponible" />Je suis disponible pour de
            nouvelles réservations</label
          >
        </div>
        <p class="muted">
          Tarif journalier : <strong>{{ guide.tarifParJour | currency: 'MAD' : 'code' }}</strong
          >. Contactez l’administrateur pour le modifier.
        </p>
        @if (submitted() && form.invalid) {
          <p class="form-errors" role="alert">
            Renseignez votre nom, votre ville, au moins une langue et un e-mail valide si fourni.
          </p>
        }
        <div class="actions">
          <button class="button primary" [disabled]="saving()">
            {{ saving() ? 'Enregistrement…' : 'Enregistrer mon profil' }}
          </button>
        </div>
      </form>
    }`,
})
export class GuideProfilePage {
  private readonly api = inject(WorkspaceService);
  readonly profile = signal<GuideProfile | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly submitted = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  readonly form = inject(FormBuilder).nonNullable.group({
    nom: ['', [Validators.required, Validators.maxLength(160)]],
    ville: ['', [Validators.required, Validators.maxLength(120)]],
    langues: ['', Validators.required],
    email: ['', Validators.email],
    telephone: '',
    disponible: true,
  });
  constructor() {
    this.load();
  }
  load() {
    this.loading.set(true);
    this.error.set('');
    this.api
      .profile()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (g) => {
          this.profile.set(g);
          this.form.patchValue({
            ...g,
            langues: g.langues?.join(', ') || '',
            email: g.email || '',
            telephone: g.telephone || '',
          });
        },
        error: (e) => this.error.set(apiError(e)),
      });
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
    this.error.set('');
    this.api
      .updateProfile({ ...raw, langues })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (g) => {
          this.profile.set(g);
          this.message.set('Votre profil a été mis à jour.');
        },
        error: (e) => this.error.set(apiError(e)),
      });
  }
}
