import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-login',
  imports: [RouterLink],
  host: { class: 'workspace-ui' },
  template: `<main class="workspace login-page">
    <section class="surface login-card" aria-labelledby="login-title">
      <p class="eyebrow">GUIDE TOURISTIQUE</p>
      <h1 id="login-title">Connexion à votre espace</h1>
      @if (connecting()) {
        <p role="status">Ouverture de la connexion sécurisée…</p>
      } @else {
        <p>Connectez-vous pour retrouver vos réservations et votre espace personnel.</p>
        @if (auth.problem() || error()) {
          <p class="error" role="alert">{{ auth.problem() || error() }}</p>
        }
        <button class="button primary" type="button" [disabled]="!!auth.problem()" (click)="connect()">Se connecter</button>
      }
      <a routerLink="/">Revenir à l’accueil</a>
    </section>
  </main>`,
})
export class Login implements OnInit {
  readonly auth = inject(AuthService);
  readonly connecting = signal(false);
  readonly error = signal('');
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  ngOnInit() {
    void this.connect();
  }

  async connect() {
    if (this.connecting() || this.auth.problem()) return;
    this.error.set('');
    this.connecting.set(true);
    try {
      if (this.auth.authenticated()) await this.router.navigateByUrl(this.auth.home());
      else await this.auth.login(this.route.snapshot.queryParamMap.get('retour') || '/mon-espace');
    } catch {
      this.error.set('La connexion n’a pas pu démarrer. Veuillez réessayer.');
    } finally {
      this.connecting.set(false);
    }
  }
}
