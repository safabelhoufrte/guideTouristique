import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';

/**
 * Espace Touriste: entry point only. The route is protected by Keycloak (role USER);
 * the real content (reservations, etc.) is built by the Reservation team.
 */
@Component({
  selector: 'app-tourist-space',
  imports: [RouterLink],
  template: `
    <main class="tourist-space">
      <h1>Espace Touriste</h1>
      <p>Connecté en tant que <strong>{{ auth.name() }}</strong>.</p>
      <p class="note">Contenu à construire par l’équipe Réservation.</p>
      <nav aria-label="Espace Touriste">
        <a routerLink="/espace-touriste/avis">Mes avis</a>
        <a routerLink="/circuits">Découvrir les circuits</a>
        <button type="button" (click)="auth.logout()">Se déconnecter</button>
      </nav>
    </main>
  `,
  styles: `
    .tourist-space {
      max-width: 720px;
      margin: 48px auto;
      padding: 0 20px;
    }
    .note {
      color: var(--muted);
    }
    nav {
      display: flex;
      flex-wrap: wrap;
      gap: 16px;
      align-items: center;
      margin-top: 24px;
    }
  `,
})
export class TouristSpace {
  readonly auth = inject(AuthService);
}
