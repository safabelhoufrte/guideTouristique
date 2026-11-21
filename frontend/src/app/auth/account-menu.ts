import { Component, ElementRef, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from './auth.service';

/**
 * Traveller account menu in the public header (avatar with initials, like big travel sites):
 * reservations, reviews, profile and sign-out, without leaving the public layout.
 */
@Component({
  selector: 'app-account-menu',
  imports: [RouterLink],
  host: {
    '(document:click)': 'onDocumentClick($event)',
    '(document:keydown.escape)': 'close()',
  },
  template: `
    <button
      type="button"
      class="avatar-button"
      aria-haspopup="menu"
      [attr.aria-expanded]="open()"
      aria-controls="account-menu"
      (click)="toggle()"
    >
      <span class="avatar" aria-hidden="true">{{ initials() }}</span>
      <span class="name">{{ firstName() }}</span>
      <span class="chevron" aria-hidden="true">▾</span>
    </button>
    @if (open()) {
      <div id="account-menu" class="menu" role="menu" aria-label="Mon compte">
        <div class="menu-head">
          <strong>{{ auth.name() }}</strong>
          <small>{{ auth.email() }}</small>
        </div>
        <a role="menuitem" routerLink="/mes-reservations">Mes réservations</a>
        <a role="menuitem" routerLink="/espace-touriste/avis">Mes avis</a>
        <a role="menuitem" routerLink="/espace-touriste/profil">Mon profil</a>
        <button role="menuitem" type="button" class="logout" (click)="auth.logout()">
          Se déconnecter
        </button>
      </div>
    }
  `,
  styles: `
    :host {
      position: relative;
      display: inline-block;
    }
    .avatar-button {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      min-height: 44px;
      padding: 4px 10px 4px 4px;
      border: 1px solid var(--panel-border);
      border-radius: 999px;
      background: #fff;
      color: var(--text);
      font: inherit;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
    }
    .avatar-button:hover,
    .avatar-button[aria-expanded='true'] {
      background: var(--panel-tint);
    }
    .avatar {
      width: 34px;
      height: 34px;
      border-radius: 50%;
      display: grid;
      place-items: center;
      background: linear-gradient(135deg, #045467, #108ca8);
      color: #fff;
      font-size: 13px;
      font-weight: 700;
    }
    .chevron {
      color: var(--muted);
      font-size: 11px;
    }
    .menu {
      position: absolute;
      right: 0;
      top: calc(100% + 8px);
      z-index: 50;
      min-width: 230px;
      background: #fff;
      border: 1px solid var(--panel-border);
      border-radius: 12px;
      padding: 6px;
      box-shadow: 0 12px 30px rgba(15, 52, 66, 0.14);
    }
    .menu-head {
      padding: 10px 12px 12px;
      border-bottom: 1px solid #edf1f2;
      margin-bottom: 6px;
    }
    .menu-head strong {
      display: block;
      font-size: 14px;
    }
    .menu-head small {
      color: var(--muted);
      font-size: 12px;
    }
    .menu a,
    .menu button {
      display: block;
      width: 100%;
      text-align: left;
      padding: 10px 12px;
      border-radius: 8px;
      border: 0;
      background: none;
      color: var(--text);
      font: inherit;
      font-size: 14px;
      text-decoration: none;
      cursor: pointer;
    }
    .menu a:hover,
    .menu button:hover,
    .menu a:focus-visible,
    .menu button:focus-visible {
      background: var(--panel-tint);
    }
    .logout {
      color: #a01831 !important;
      border-top: 1px solid #edf1f2 !important;
      margin-top: 6px;
      border-radius: 0 0 8px 8px !important;
    }
    @media (max-width: 700px) {
      .name,
      .chevron {
        display: none;
      }
      .avatar-button {
        padding: 4px;
      }
    }
  `,
})
export class AccountMenu {
  readonly auth = inject(AuthService);
  private readonly host = inject(ElementRef<HTMLElement>);
  readonly open = signal(false);

  readonly initials = computed(() => {
    const parts = this.auth.name().trim().split(/\s+/).filter(Boolean);
    return ((parts[0]?.[0] ?? 'V') + (parts[1]?.[0] ?? '')).toUpperCase();
  });
  readonly firstName = computed(() => this.auth.name().trim().split(/\s+/)[0] ?? '');

  constructor() {
    inject(Router)
      .events.pipe(
        filter((e) => e instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.close());
  }

  toggle() {
    this.open.update((v) => !v);
  }

  close() {
    this.open.set(false);
  }

  onDocumentClick(event: MouseEvent) {
    if (!this.host.nativeElement.contains(event.target as Node)) this.close();
  }
}
