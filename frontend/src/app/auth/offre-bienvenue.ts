import { HttpClient } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from './auth.service';

interface OffreReponse {
  valide: boolean;
  code: string;
  pourcentage: number;
  message: string;
}

/**
 * Affiche le code de bienvenue a un touriste connecte qui n'a pas encore reserve.
 * Le code vient de l'admin (page Codes promo) ; le bandeau disparait apres la 1re reservation.
 */
@Component({
  selector: 'app-offre-bienvenue',
  imports: [RouterLink],
  template: `@if (offre(); as o) {
    @if (!ferme()) {
      <aside class="offre" role="status">
        <p>
          <strong>Bienvenue !</strong> Pour votre première réservation, utilisez le code
          <span class="code">{{ o.code }}</span> : <strong>-{{ o.pourcentage }} %</strong>.
        </p>
        <div class="actions">
          <button type="button" (click)="copier(o.code)">
            {{ copie() ? 'Copié !' : 'Copier le code' }}
          </button>
          <a routerLink="/circuits">Choisir un circuit</a>
          <button type="button" class="close" aria-label="Fermer" (click)="fermer()">×</button>
        </div>
      </aside>
    }
  }`,
  styles: `
    .offre {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      padding: 12px 56px;
      background: linear-gradient(90deg, #0e7490, #38a3bd);
      color: #fff;
    }
    p { margin: 0; }
    .code {
      background: rgba(255, 255, 255, 0.2);
      border: 1px dashed #fff;
      border-radius: 6px;
      padding: 2px 10px;
      font-weight: 700;
      letter-spacing: 0.08em;
    }
    .actions { display: flex; align-items: center; gap: 14px; }
    button, a {
      background: #fff;
      color: #0e7490;
      border: 0;
      border-radius: 8px;
      padding: 8px 14px;
      font-weight: 600;
      text-decoration: none;
      cursor: pointer;
    }
    .close { background: transparent; color: #fff; font-size: 22px; padding: 0 6px; }
    @media (max-width: 700px) { .offre { padding: 12px 16px; } }
  `,
})
export class OffreBienvenue implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  readonly offre = signal<OffreReponse | null>(null);
  readonly ferme = signal(false);
  readonly copie = signal(false);

  private cle() {
    return 'offre-bienvenue-fermee:' + this.auth.email();
  }

  ngOnInit() {
    const email = this.auth.email();
    if (!this.auth.has('USER') || !email) return;
    try {
      if (localStorage.getItem(this.cle()) === '1') return;
    } catch {
      /* stockage indisponible : on affiche quand meme */
    }
    this.http
      .get<OffreReponse>('/api/reservations/offres/bienvenue', { params: { email } })
      .subscribe({
        next: (r) => {
          if (r.valide) this.offre.set(r);
        },
        error: () => {},
      });
  }

  fermer() {
    this.ferme.set(true);
    try {
      localStorage.setItem(this.cle(), '1');
    } catch {
      /* ignore */
    }
  }

  copier(code: string) {
    navigator.clipboard?.writeText(code).then(() => this.copie.set(true));
  }
}