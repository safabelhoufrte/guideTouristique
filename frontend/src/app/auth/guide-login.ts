import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { GuideAuthService } from
  '../services/guide-auth.service';

@Component({
  selector: 'app-guide-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],

  template: `
    <div class="login-page">

      <form class="login-card"
            #loginForm="ngForm"
            (ngSubmit)="login()">

        <div class="logo">G</div>

        <h1>Connexion Guide</h1>

        <p>
          Accédez à votre espace professionnel
          Guide Touristique.
        </p>

        <label for="email">Adresse email</label>

        <input
          id="email"
          type="email"
          name="email"
          [(ngModel)]="email"
          placeholder="guide@example.com"
          autocomplete="username"
          required
          email
        />

        <label for="password">Mot de passe</label>

        <input
          id="password"
          type="password"
          name="password"
          [(ngModel)]="password"
          placeholder="Votre mot de passe"
          autocomplete="current-password"
          required
        />

        <div class="error" *ngIf="errorMessage"
             role="alert">
          {{ errorMessage }}
        </div>

        <button type="submit"
                [disabled]="loading || loginForm.invalid">
          {{ loading ? 'Connexion...' : 'Se connecter' }}
        </button>

        <a routerLink="/">Retour à l'accueil</a>

      </form>
    </div>
  `,

  styles: [`
    .login-page {
      min-height: 100vh;
      background: #f1f6f7;
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 20px;
      font-family: Arial, sans-serif;
    }

    .login-card {
      background: white;
      width: 100%;
      max-width: 420px;
      padding: 38px;
      border-radius: 14px;
      display: flex;
      flex-direction: column;
      gap: 15px;
      box-shadow: 0 10px 35px #123b461a;
    }

    .logo {
      width: 52px;
      height: 52px;
      background: #0e7490;
      color: white;
      border-radius: 12px;
      display: grid;
      place-items: center;
      font-size: 28px;
      font-weight: bold;
    }

    h1 {
      color: #123b46;
      margin: 8px 0 0;
      font-size: 27px;
    }

    p {
      color: #64748b;
      line-height: 1.6;
    }

    label {
      color: #334155;
      font-size: 14px;
      font-weight: 600;
    }

    input {
      padding: 13px;
      border: 1px solid #cbd5e1;
      border-radius: 8px;
      font-size: 14px;
    }

    input:focus {
      outline: 2px solid #0e7490;
    }

    button {
      background: #0e7490;
      color: white;
      padding: 14px;
      border: none;
      border-radius: 8px;
      font-weight: 600;
      cursor: pointer;
      margin-top: 10px;
    }

    button:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .error {
      color: #b42318;
      font-size: 13px;
    }

    a {
      text-align: center;
      color: #0e7490;
      text-decoration: none;
      font-size: 13px;
    }
  `]
})
export class GuideLogin {

  private auth = inject(GuideAuthService);
  private router = inject(Router);

  email = '';
  password = '';
  loading = false;
  errorMessage = '';

  login(): void {
    this.loading = true;
    this.errorMessage = '';

    this.auth.login(this.email, this.password)
      .subscribe({
        next: () => {
          this.loading = false;
          this.password = '';
          this.router.navigate(['/espace-guide']);
        },

        error: (error) => {
          this.loading = false;

          this.errorMessage =
            error.status === 401
              ? 'Email ou mot de passe incorrect.'
              : 'Connexion impossible. Vérifiez le serveur Guide.';
        }
      });
  }
}