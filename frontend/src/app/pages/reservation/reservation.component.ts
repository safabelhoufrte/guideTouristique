import { ChangeDetectorRef, Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { catchError, combineLatest, finalize, of, switchMap, tap } from 'rxjs';
import { CircuitResume } from '../../models/reservation';
import { ReservationService } from '../../services/reservation.service';

@Component({
  selector: 'app-reservation',
  imports: [ReactiveFormsModule],
  templateUrl: './reservation.component.html',
  styleUrl: './reservation.component.scss',
})
export class ReservationComponent implements OnInit {
  private readonly service = inject(ReservationService);
  private readonly route = inject(ActivatedRoute);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  circuits: CircuitResume[] = [];
  nbOptions = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
  guideNom = '';
  statut = '';
  message = '';
  erreur = '';
  soumis = false;
  envoi = false;
  confirmationId: number | null = null;
  chargement = true;
  readonly aujourdHui = new Date().toLocaleDateString('en-CA');

  form = inject(FormBuilder).nonNullable.group({
    circuitId: [0, [Validators.required, Validators.min(1)]],
    dateVisite: ['', Validators.required],
    nbPersonnes: [2, [Validators.required, Validators.min(1)]],
    nom: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
  });

  ngOnInit() {
    this.form.controls.circuitId.valueChanges
      .pipe(
        tap(() => {
          this.guideNom = '';
          this.confirmationId = null;
          this.message = '';
          this.statut = '';
        }),
        switchMap(() => {
          const circuit = this.circuitChoisi;
          return circuit
            ? this.service.getGuide(circuit.guideId).pipe(catchError(() => of(null)))
            : of(null);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((guide) => {
        this.guideNom = guide?.nom ?? 'Informations indisponibles';
        this.cdr.markForCheck();
      });
    combineLatest([this.service.getCircuits(), this.route.queryParamMap])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ([circuits, params]) => {
          this.circuits = circuits;
          this.chargement = false;
          const requestedId = Number(params.get('circuitId'));
          const selected = params.has('circuitId')
            ? circuits.find((c) => c.id === requestedId)
            : circuits[0];
          this.form.patchValue({ circuitId: selected?.id ?? 0 });
          if (params.has('circuitId') && !selected)
            this.erreur = 'Ce circuit n’est plus disponible. Choisissez un autre circuit.';
          this.cdr.markForCheck();
        },
        error: () => {
          this.chargement = false;
          this.erreur = 'Impossible de charger les circuits.';
          this.cdr.markForCheck();
        },
      });
  }

  get circuitChoisi() {
    return this.circuits.find((c) => c.id === this.form.controls.circuitId.value);
  }
  get nbPersonnes() {
    return Number(this.form.controls.nbPersonnes.value) || 0;
  }
  get total() {
    return (this.circuitChoisi?.prix ?? 0) * this.nbPersonnes;
  }

  confirmer() {
    if (this.envoi || this.confirmationId !== null) return;
    this.soumis = true;
    this.message = '';
    this.erreur = '';
    if (this.form.invalid || !this.circuitChoisi) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.form.controls.dateVisite.value < this.aujourdHui) {
      this.erreur = 'Choisissez une date à partir d’aujourd’hui.';
      return;
    }
    this.envoi = true;
    this.service
      .creer(this.form.getRawValue())
      .pipe(
        finalize(() => {
          this.envoi = false;
          this.cdr.markForCheck();
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (reservation) => {
          this.confirmationId = reservation.id;
          this.statut = reservation.statut;
          this.message = `Réservation n° ${reservation.id} enregistrée ! Montant : ${reservation.prixTotal} MAD.`;
        },
        error: (error) => {
          this.erreur =
            error.status === 503 || error.status === 0
              ? 'Un service est indisponible, réessayez plus tard.'
              : 'Impossible de créer la réservation. Vérifiez la date et la disponibilité du guide.';
        },
      });
  }
}
