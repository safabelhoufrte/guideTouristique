import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-space-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  host: { class: 'workspace-ui' },
  template: `<section class="workspace-banner">
      <div class="workspace">
        <p class="eyebrow">{{ title }}</p>
        <h1>Bonjour, {{ auth.name() }}</h1>
        <p>{{ subtitle }}</p>
      </div>
    </section>
    <div class="workspace workspace-layout">
      <aside class="space-nav surface">
        <p class="nav-caption">MON ESPACE</p>
        <nav aria-label="Navigation de votre espace">
          @for (item of links; track item.path) {
            <a
              [routerLink]="item.path"
              routerLinkActive="selected"
              [routerLinkActiveOptions]="{ exact: true }"
              ariaCurrentWhenActive="page"
              >{{ item.label }}<span aria-hidden="true">→</span></a
            >
          }
        </nav>
        <a class="back-public" routerLink="/circuits">Découvrir les circuits</a>
      </aside>
      <div class="space-content"><router-outlet /></div>
    </div>`,
})
export class SpaceShell {
  readonly auth = inject(AuthService);
  private readonly mode = inject(ActivatedRoute).snapshot.data['space'] as string;
  readonly title =
    this.mode === 'admin'
      ? 'ADMINISTRATION'
      : this.mode === 'guide'
        ? 'ESPACE GUIDE'
        : 'ESPACE TOURISTE';
  readonly subtitle =
    this.mode === 'admin'
      ? 'Pilotez les circuits, les guides et les demandes de vos visiteurs.'
      : this.mode === 'guide'
        ? 'Retrouvez votre profil, vos circuits et les réservations qui vous concernent.'
        : 'Toutes vos découvertes, vos réservations et vos souvenirs au même endroit.';
  readonly links =
    this.mode === 'admin'
      ? [
          { path: '/administration', label: 'Vue d’ensemble' },
          { path: '/administration/circuits', label: 'Circuits' },
          { path: '/administration/guides', label: 'Guides' },
          { path: '/administration/reservations', label: 'Réservations' },
          { path: '/administration/avis', label: 'Modération des avis' },
        ]
      : this.mode === 'guide'
        ? [
            { path: '/espace-guide', label: 'Mon profil' },
            { path: '/espace-guide/circuits', label: 'Mes circuits' },
            { path: '/espace-guide/reservations', label: 'Mes visiteurs' },
          ]
        : [
            { path: '/espace-touriste', label: 'Mes réservations' },
            { path: '/espace-touriste/avis', label: 'Mes avis' },
          ];
}
