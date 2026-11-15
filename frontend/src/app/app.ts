import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs';
import { AuthService } from './auth/auth.service';
import { NavigationEnd, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Router } from '@angular/router';
import { AuthDialog, AuthDialogService } from './auth/auth-dialog';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, AuthDialog],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  readonly auth = inject(AuthService);
  readonly dialog = inject(AuthDialogService);
  private readonly router = inject(Router);
  /** Back-office pages use their own full-screen layout (sidebar + topbar), like the Guide dashboard. */
  readonly consoleSpace = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map(() => this.router.url.startsWith('/administration')),
    ),
    { initialValue: this.router.url.startsWith('/administration') },
  );

  openLogin() {
    this.dialog.open(this.router.parseUrl(this.router.url).queryParams['retour'] || '/mon-espace');
  }
}
