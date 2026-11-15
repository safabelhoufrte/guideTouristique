import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  ActivatedRoute,
  NavigationEnd,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from '@angular/router';
import { filter, map } from 'rxjs';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-space-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  host: { class: 'workspace-ui' },
  template: `@if (mode === 'admin') {
      <div class="admin-console">
        <aside class="console-sidebar">
          <div class="console-brand">
            <div class="console-brand-icon" aria-hidden="true">G</div>
            <div>
              <strong>Guide Touristique</strong><small>Administration</small>
            </div>
          </div>
          <p class="console-menu-label">MENU PRINCIPAL</p>
          <nav class="console-navigation" aria-label="Navigation de l’administration">
            @for (item of links; track item.path) {
              <a
                class="console-nav-item"
                [routerLink]="item.path"
                routerLinkActive="active"
                [routerLinkActiveOptions]="{ exact: true }"
                ariaCurrentWhenActive="page"
                ><span aria-hidden="true">{{ item.icon }}</span> {{ item.label }}</a
              >
            }
          </nav>
          <div class="console-help">
            <strong>Catalogue public</strong>
            <p>Vérifiez le rendu des circuits tel que les voyageurs le voient.</p>
            <a routerLink="/circuits">Découvrir les circuits ›</a>
          </div>
          <div class="console-footer">© 2026 Guide Touristique</div>
          <button type="button" class="console-logout" (click)="auth.logout()">
            ⎋ Se déconnecter
          </button>
        </aside>
        <div class="console-main">
          <header class="console-topbar">
            <div>
              <span class="console-breadcrumb">Administration / </span
              ><strong>{{ pageTitle() }}</strong>
            </div>
            <div class="console-user">
              <span class="console-avatar" aria-hidden="true">{{ initial() }}</span
              ><strong>{{ auth.name() }}</strong>
            </div>
          </header>
          <section class="console-content"><router-outlet /></section>
        </div>
      </div>
    } @else {
      <section class="workspace-banner">
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
      </div>
    }`,
})
export class SpaceShell {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly mode = inject(ActivatedRoute).snapshot.data['space'] as string;
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
          { path: '/administration', label: 'Tableau de bord', icon: '▦' },
          { path: '/administration/circuits', label: 'Circuits', icon: '⌖' },
          { path: '/administration/guides', label: 'Guides', icon: '♙' },
          { path: '/administration/reservations', label: 'Réservations', icon: '▤' },
          { path: '/administration/avis', label: 'Modération des avis', icon: '☆' },
        ]
      : this.mode === 'guide'
        ? [
            { path: '/espace-guide', label: 'Mon profil', icon: '' },
            { path: '/espace-guide/circuits', label: 'Mes circuits', icon: '' },
            { path: '/espace-guide/reservations', label: 'Mes visiteurs', icon: '' },
          ]
        : [
            { path: '/espace-touriste', label: 'Mes réservations', icon: '' },
            { path: '/espace-touriste/avis', label: 'Mes avis', icon: '' },
          ];
  private readonly url = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map(() => this.router.url),
    ),
    { initialValue: this.router.url },
  );
  /** Topbar title: the deepest menu entry matching the current URL. */
  pageTitle() {
    const path = this.url().split(/[?#]/)[0];
    const match = [...this.links]
      .sort((a, b) => b.path.length - a.path.length)
      .find((item) => path === item.path || path.startsWith(item.path + '/'));
    return match?.label ?? this.title;
  }
  initial() {
    return (this.auth.name().trim()[0] ?? 'A').toUpperCase();
  }
}
