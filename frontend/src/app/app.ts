import { Component, inject } from '@angular/core';
import { AuthService } from './auth/auth.service';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
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

  openLogin() {
    this.dialog.open(this.router.parseUrl(this.router.url).queryParams['retour'] || '/mon-espace');
  }
}
