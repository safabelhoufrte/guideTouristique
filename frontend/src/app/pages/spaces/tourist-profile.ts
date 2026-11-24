import { Component, computed, inject } from '@angular/core';
import { AuthService } from '../../auth/auth.service';

/**
 * Espace Touriste, "Mon profil": the traveller's account comes from Keycloak.
 * Changes go through Keycloak's own pages (themed like the sign-in), then come back here.
 */
@Component({
  selector: 'app-tourist-profile',
  template: `
    <section class="head">
      <div>
        <p class="eyebrow">VOTRE COMPTE</p>
        <h1>Mon profil</h1>
        <p>Les informations utilisées pour vos réservations et vos avis.</p>
      </div>
    </section>

    @if (message(); as m) {
      <p class="notice" [class.error]="m.error" role="status">{{ m.text }}</p>
    }

    <section class="card identity">
      <div class="avatar" aria-hidden="true">{{ initials() }}</div>
      <div>
        <h2>{{ auth.name() }}</h2>
        <p>{{ auth.email() || 'Adresse e-mail non renseignée' }}</p>
        <span class="badge">Voyageur</span>
      </div>
    </section>

    <div class="grid">
      <section class="card">
        <div class="card-head">
          <h2>Informations personnelles</h2>
          <button type="button" class="primary" (click)="edit('UPDATE_PROFILE')">
            Modifier mes informations
          </button>
        </div>
        <dl>
          <div>
            <dt>Prénom</dt>
            <dd>{{ auth.account().firstName || '—' }}</dd>
          </div>
          <div>
            <dt>Nom</dt>
            <dd>{{ auth.account().lastName || '—' }}</dd>
          </div>
          <div>
            <dt>Adresse e-mail</dt>
            <dd>
              {{ auth.email() || '—' }}
              @if (auth.email()) {
                <small [class.ok]="auth.account().emailVerified">
                  {{ auth.account().emailVerified ? 'vérifiée' : 'non vérifiée' }}
                </small>
              }
            </dd>
          </div>
          <div>
            <dt>Identifiant de connexion</dt>
            <dd>{{ auth.account().username || '—' }}</dd>
          </div>
        </dl>
        <p class="hint">
          Votre nom et votre e-mail sont repris pour vos prochaines réservations et vos avis.
        </p>
      </section>

      <section class="card">
        <h2>Sécurité</h2>
        <p class="text">
          Votre mot de passe est géré de façon sécurisée par le service d’authentification. Il n’est
          jamais visible par l’application.
        </p>
        <button type="button" class="secondary" (click)="edit('UPDATE_PASSWORD')">
          Changer mon mot de passe
        </button>
        <h2 class="spaced">Session</h2>
        <p class="text">Fermez votre session sur cet appareil.</p>
        <button type="button" class="secondary" (click)="auth.logout()">Se déconnecter</button>
      </section>
    </div>
  `,
  styles: `
    :host {
      display: block;
      max-width: 1100px;
      margin: 36px auto 56px;
      padding: 0 20px;
    }
    .head {
      margin-bottom: 18px;
    }
    .head h1 {
      margin: 6px 0;
      font-size: 26px;
    }
    .head p {
      margin: 0;
      color: var(--muted);
    }
    .eyebrow {
      font-size: 11px;
      letter-spacing: 1px;
      color: var(--primary) !important;
      font-weight: 700;
    }
    .notice {
      background: #e5f6ea;
      border: 1px solid #bfe5cb;
      color: #1d6b49;
      padding: 12px 14px;
      border-radius: 8px;
    }
    .notice.error {
      background: #fcecec;
      border-color: #ffcccc;
      color: #941c1c;
    }
    .card {
      background: #fff;
      border: 1px solid var(--panel-border);
      border-radius: 11px;
      padding: 20px;
    }
    .card h2 {
      font-size: 17px;
      margin: 0 0 12px;
    }
    .identity {
      display: flex;
      gap: 18px;
      align-items: center;
      margin-bottom: 18px;
    }
    .identity h2 {
      margin: 0 0 4px;
      font-size: 20px;
    }
    .identity p {
      margin: 0 0 8px;
      color: var(--muted);
    }
    .avatar {
      width: 72px;
      height: 72px;
      border-radius: 50%;
      display: grid;
      place-items: center;
      background: linear-gradient(135deg, #045467, #108ca8);
      color: #fff;
      font-size: 26px;
      font-weight: 700;
    }
    .badge {
      font-size: 11px;
      font-weight: 700;
      padding: 4px 8px;
      border-radius: 6px;
      background: #e7f7fa;
      color: #117c91;
    }
    .grid {
      display: grid;
      grid-template-columns: 1.4fr 1fr;
      gap: 18px;
      align-items: start;
    }
    .card-head {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 12px;
      margin-bottom: 8px;
    }
    .card-head h2 {
      margin: 0;
    }
    dl {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px 24px;
      margin: 16px 0 0;
    }
    dt {
      font-size: 12px;
      font-weight: 600;
      color: var(--muted);
      margin-bottom: 4px;
    }
    dd {
      margin: 0;
      font-weight: 500;
    }
    dd small {
      margin-left: 6px;
      font-size: 11px;
      color: #a67d21;
    }
    dd small.ok {
      color: #217b54;
    }
    .hint,
    .text {
      color: var(--muted);
      font-size: 13px;
      line-height: 1.6;
    }
    .spaced {
      margin-top: 22px !important;
    }
    button {
      border: 0;
      border-radius: 8px;
      padding: 11px 16px;
      font-weight: 600;
      cursor: pointer;
      font: inherit;
    }
    .primary {
      background: var(--primary);
      color: #fff;
    }
    .primary:hover {
      background: var(--primary-hover);
    }
    .secondary {
      background: #fff;
      color: var(--primary);
      border: 1px solid var(--panel-border);
    }
    .secondary:hover {
      background: var(--panel-tint);
    }
    @media (max-width: 900px) {
      .grid,
      dl {
        grid-template-columns: 1fr;
      }
      .card-head {
        flex-direction: column;
        align-items: flex-start;
      }
    }
  `,
})
export class TouristProfile {
  readonly auth = inject(AuthService);
  private readonly returnUrl = '/espace-touriste/profil';

  readonly initials = computed(() => {
    const parts = this.auth.name().trim().split(/\s+/).filter(Boolean);
    return ((parts[0]?.[0] ?? 'V') + (parts[1]?.[0] ?? '')).toUpperCase();
  });

  /** Message after coming back from Keycloak (success, or cancelled by the traveller). */
  readonly message = computed(() => {
    const done = this.auth.accountAction();
    if (!done) return null;
    if (done.status === 'success') {
      const text =
        done.action === 'UPDATE_PASSWORD'
          ? 'Votre mot de passe a bien été modifié.'
          : 'Vos informations ont bien été mises à jour.';
      return { text, error: false };
    }
    if (done.status === 'cancelled') return { text: 'Modification annulée.', error: false };
    return { text: 'La modification n’a pas pu être enregistrée. Réessayez.', error: true };
  });

  edit(action: 'UPDATE_PROFILE' | 'UPDATE_PASSWORD') {
    void this.auth.manageAccount(action, this.returnUrl);
  }
}
