import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CircuitResume } from '../../models/reservation';
import { ReservationService } from '../../services/reservation.service';

// Provisoire : en attendant un vrai service de codes promo côté backend
const CODES_PROMO: Record<string, number> = { PROMO10: 10, BIENVENUE: 15 };

@Component({
  selector: 'app-reservation',
  imports: [ReactiveFormsModule],
  templateUrl: './reservation.component.html',
  styleUrl: './reservation.component.scss',
})
export class ReservationComponent implements OnInit {
  private fb = inject(FormBuilder);
  private service = inject(ReservationService);

  circuits: CircuitResume[] = [];
  nbOptions = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
  guideNom = '';
  statut = 'EN_ATTENTE';
  message = '';
  erreur = '';
  soumis = false;

  reduction = 0; // en %
  promoMessage = '';
  promoOk = false;

  form = this.fb.nonNullable.group({
    circuitId: [0, Validators.required],
    dateVisite: ['', Validators.required],
    nbPersonnes: [2, [Validators.required, Validators.min(1)]],
    nom: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    codePromo: [''],
  });

  ngOnInit() {
    this.service.getCircuits().subscribe({
      next: (c) => {
        this.circuits = c;
        if (c.length) {
          this.form.patchValue({ circuitId: c[0].id });
          this.chargerGuide();
        }
      },
      error: () => (this.erreur = 'Impossible de charger les circuits.'),
    });
    this.form.controls.circuitId.valueChanges.subscribe(() => this.chargerGuide());
  }

  get circuitChoisi(): CircuitResume | undefined {
    return this.circuits.find((c) => c.id === this.form.controls.circuitId.value);
  }

  get nbPersonnes(): number {
    return Number(this.form.controls.nbPersonnes.value) || 0;
  }

  get sousTotal(): number {
    return (this.circuitChoisi?.prix ?? 0) * this.nbPersonnes;
  }

  get total(): number {
    return Math.round(this.sousTotal * (1 - this.reduction / 100));
  }

  chargerGuide() {
    const c = this.circuitChoisi;
    if (!c) return;
    this.service.getGuide(c.guideId).subscribe({
      next: (g) => (this.guideNom = g.nom),
      error: () => (this.guideNom = '—'),
    });
  }

  appliquerPromo() {
    const code = this.form.controls.codePromo.value.trim().toUpperCase();
    if (!code) return;
    const pct = CODES_PROMO[code];
    if (pct) {
      this.reduction = pct;
      this.promoOk = true;
      this.promoMessage = `Code appliqué : -${pct} %`;
    } else {
      this.reduction = 0;
      this.promoOk = false;
      this.promoMessage = 'Code promo invalide.';
    }
  }

  confirmer() {
    this.soumis = true;
    this.message = '';
    this.erreur = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { codePromo, ...requete } = this.form.getRawValue();
    this.service.creer(requete).subscribe({
      next: (r) => {
        this.statut = r.statut;
        this.message = 'Réservation enregistrée !';
      },
      error: (e) =>
        (this.erreur =
          e.status === 503
            ? 'Un service est indisponible, réessayez plus tard.'
            : 'Impossible de créer la réservation.'),
    });
  }
}