import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from './auth.service';
@Component({
  selector: 'app-forbidden',
  imports: [RouterLink],
  host: { class: 'workspace-ui' },
  template: `<main class="workspace">
    <section class="surface empty">
      <h1>Accès non autorisé</h1>
      <p>Votre compte ne dispose pas des permissions nécessaires pour cette page.</p>
      <a class="button primary" routerLink="/circuits">Retour aux circuits</a
      ><button class="button secondary" (click)="auth.logout()">Changer de compte</button>
    </section>
  </main>`,
})
export class Forbidden {
  readonly auth = inject(AuthService);
}
