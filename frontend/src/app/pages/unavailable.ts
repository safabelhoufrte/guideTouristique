import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-unavailable',
  imports: [RouterLink],
  template: `<main>
    <h1>Cette page sera bientôt disponible</h1>
    <p>En attendant, découvrez les circuits proposés par nos guides.</p>
    <a routerLink="/circuits">Découvrir les circuits</a>
  </main>`,
  styles: `
    main {
      max-width: 720px;
      margin: 64px auto;
      padding: 24px;
    }
    p {
      color: var(--muted);
    }
    a {
      color: var(--primary);
      display: inline-block;
      padding: 12px 0;
    }
  `,
})
export class Unavailable {}
