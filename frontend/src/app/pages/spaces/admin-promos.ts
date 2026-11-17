import { DatePipe } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { apiError } from '../../services/workspace.service';

interface PromoCode {
  code: string;
  pourcentage: number;
  actif: boolean;
  dateExpiration: string | null;
  utilisationsMax: number | null;
  utilisations: number;
  personnesMin: number;
  premiereReservationSeulement: boolean;
  bienvenue: boolean;
}

const BASE = '/api/reservations/admin/promo';

@Component({
  selector: 'app-admin-promos',
  imports: [ReactiveFormsModule, DatePipe],
  host: { class: 'workspace-ui' },
  template: `<header class="section-header">
      <div>
        <h2>Codes promo</h2>
        <p>Créez les réductions proposées aux touristes et choisissez le code de bienvenue.</p>
      </div>
    </header>
    @if (message()) {
      <p class="success" role="status">{{ message() }}</p>
    }
    @if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
    }
    <form class="surface" [formGroup]="form" (ngSubmit)="creer()">
      <h3 class="panel-heading">Nouveau code</h3>
      <div class="form-grid">
        <div>
          <label for="p-code">Code</label>
          <input id="p-code" formControlName="code" maxlength="40" placeholder="ETE2027" />
        </div>
        <div>
          <label for="p-pct">Réduction (%)</label>
          <input id="p-pct" type="number" min="1" max="100" formControlName="pourcentage" />
        </div>
        <div>
          <label for="p-min">Nombre de personnes minimum</label>
          <input id="p-min" type="number" min="1" formControlName="personnesMin" />
        </div>
        <div>
          <label for="p-exp">Date d'expiration (optionnelle)</label>
          <input id="p-exp" type="date" formControlName="dateExpiration" />
        </div>
        <div>
          <label for="p-max">Utilisations maximum (optionnel)</label>
          <input id="p-max" type="number" min="1" formControlName="utilisationsMax" />
        </div>
        <div class="full">
          <label style="display:flex;align-items:center;gap:10px"><input type="checkbox" style="width:20px;height:20px;margin:0" formControlName="premiereReservationSeulement" />
            Réservé à la première réservation du touriste</label
          >
        </div>
        <div class="full">
          <label style="display:flex;align-items:center;gap:10px"><input type="checkbox" style="width:20px;height:20px;margin:0" formControlName="bienvenue" /> Code de bienvenue : affiché aux
            nouveaux touristes</label
          >
        </div>
      </div>
      @if (submitted() && form.invalid) {
        <p class="form-errors" role="alert">
          Code de 3 à 40 caractères (lettres, chiffres, - ou _), réduction entre 1 et 100.
        </p>
      }
      <div class="actions">
        <button class="button primary" [disabled]="busy()">Créer le code</button>
      </div>
    </form>

    @if (pending(); as p) {
      <section class="confirm-box">
        <h3>Supprimer le code {{ p.code }} ?</h3>
        <p>Les réservations déjà faites avec ce code ne changent pas.</p>
        <div class="actions">
          <button class="button danger" [disabled]="busy()" (click)="supprimer(p)">Supprimer</button
          ><button class="button secondary" (click)="pending.set(null)">Conserver</button>
        </div>
      </section>
    }

    @if (loading()) {
      <p class="surface" role="status">Chargement…</p>
    } @else if (!codes().length) {
      <section class="surface empty"><h3>Aucun code promo</h3></section>
    } @else {
      <div class="surface table-wrap" style="margin-top: 22px">
        <table>
          <thead>
            <tr>
              <th>Code</th>
              <th>Réduction</th>
              <th>Conditions</th>
              <th>Utilisations</th>
              <th>Statut</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            @for (c of codes(); track c.code) {
              <tr>
                <td>
                  <strong>{{ c.code }}</strong>
                  @if (c.bienvenue) {
                    <small>Code de bienvenue</small>
                  }
                </td>
                <td>-{{ c.pourcentage }} %</td>
                <td>
                  <small>À partir de {{ c.personnesMin }} pers.</small>
                  @if (c.premiereReservationSeulement) {
                    <small>1re réservation seulement</small>
                  }
                  @if (c.dateExpiration) {
                    <small>Expire le {{ c.dateExpiration | date: 'dd/MM/yyyy' }}</small>
                  }
                </td>
                <td>{{ c.utilisations }}{{ c.utilisationsMax ? ' / ' + c.utilisationsMax : '' }}</td>
                <td>{{ c.actif ? 'Actif' : 'Désactivé' }}</td>
                <td class="table-actions">
                  <button class="button secondary" [disabled]="busy()" (click)="basculer(c)">
                    {{ c.actif ? 'Désactiver' : 'Activer' }}
                  </button>
                  @if (!c.bienvenue) {
                    <button class="button secondary" [disabled]="busy()" (click)="bienvenue(c)">
                      Code de bienvenue
                    </button>
                  }
                  <button class="button danger" [disabled]="busy()" (click)="pending.set(c)">
                    Supprimer
                  </button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }`,
})
export class AdminPromos {
  private readonly http = inject(HttpClient);
  readonly codes = signal<PromoCode[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  readonly submitted = signal(false);
  readonly pending = signal<PromoCode | null>(null);
  readonly form = inject(FormBuilder).nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9_-]{3,40}$/)]],
    pourcentage: [10, [Validators.required, Validators.min(1), Validators.max(100)]],
    personnesMin: [1, [Validators.required, Validators.min(1)]],
    dateExpiration: [''],
    utilisationsMax: [null as number | null],
    premiereReservationSeulement: [false],
    bienvenue: [false],
  });

  constructor() {
    this.charger();
  }

  charger() {
    this.http.get<PromoCode[]>(BASE).subscribe({
      next: (rows) => {
        this.codes.set(rows);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(apiError(e));
      },
    });
  }

  private ok(texte: string) {
    this.busy.set(false);
    this.error.set('');
    this.message.set(texte);
    this.charger();
  }
  private ko(e: unknown) {
    this.busy.set(false);
    this.message.set('');
    this.error.set(apiError(e));
  }

  creer() {
    this.submitted.set(true);
    if (this.form.invalid || this.busy()) return;
    this.busy.set(true);
    const v = this.form.getRawValue();
    this.http.post<PromoCode>(BASE, { ...v, dateExpiration: v.dateExpiration || null }).subscribe({
      next: (c) => {
        this.form.reset({
          code: '',
          pourcentage: 10,
          personnesMin: 1,
          dateExpiration: '',
          utilisationsMax: null,
          premiereReservationSeulement: false,
          bienvenue: false,
        });
        this.submitted.set(false);
        this.ok('Le code ' + c.code + ' a été créé.');
      },
      error: (e) => {
        this.busy.set(false);
        this.message.set('');
        this.error.set(e?.status === 409 ? 'Ce code existe déjà.' : apiError(e));
      },
    });
  }

  basculer(c: PromoCode) {
    this.busy.set(true);
    const params = new HttpParams().set('actif', !c.actif);
    this.http.patch(`${BASE}/${c.code}/actif`, {}, { params }).subscribe({
      next: () => this.ok(c.actif ? 'Code désactivé.' : 'Code activé.'),
      error: (e) => this.ko(e),
    });
  }

  bienvenue(c: PromoCode) {
    this.busy.set(true);
    this.http.patch(`${BASE}/${c.code}/bienvenue`, {}).subscribe({
      next: () => this.ok(c.code + ' est maintenant le code de bienvenue.'),
      error: (e) => this.ko(e),
    });
  }

  supprimer(c: PromoCode) {
    this.busy.set(true);
    this.http.delete(`${BASE}/${c.code}`).subscribe({
      next: () => {
        this.pending.set(null);
        this.ok('Le code ' + c.code + ' a été supprimé.');
      },
      error: (e) => {
        this.pending.set(null);
        this.ko(e);
      },
    });
  }
}