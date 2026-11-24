import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, of, timer, switchMap } from 'rxjs';
import { AuthService } from '../../auth/auth.service';
import { Circuit, Guide } from '../../models/circuit';
import { circuitPhoto } from '../../models/circuit-photo';
import { Reservation } from '../../models/reservation';
import { CircuitService } from '../../services/circuit.service';
import { GuideService } from '../../services/guide.service';
import { ReservationService } from '../../services/reservation.service';

type Onglet = 'attente' | 'confirmees' | 'annulees';

const ONGLETS: { cle: Onglet; label: string; statuts: string[] }[] = [
  { cle: 'attente', label: 'En attente', statuts: ['EN_ATTENTE'] },
  { cle: 'confirmees', label: 'Confirmées', statuts: ['ACCEPTEE'] },
  { cle: 'annulees', label: 'Annulées', statuts: ['ANNULEE', 'REFUSEE'] },
];

@Component({
  selector: 'app-mes-reservations',
  imports: [CurrencyPipe, DatePipe, FormsModule, RouterLink],
  templateUrl: './mes-reservations.html',
  styleUrl: './mes-reservations.scss',
})
export class MesReservations implements OnInit {
  private readonly reservations = inject(ReservationService);
  private readonly circuitsApi = inject(CircuitService);
  private readonly guidesApi = inject(GuideService);
  private readonly destroyRef = inject(DestroyRef);
  readonly auth = inject(AuthService);

  readonly onglets = ONGLETS;
  readonly onglet = signal<Onglet>('attente');
  readonly recherche = signal('');
  readonly lignes = signal<Reservation[]>([]);
  readonly circuits = signal<Map<number, Circuit>>(new Map());
  readonly guides = signal<Map<number, Guide>>(new Map());
  readonly chargement = signal(true);
  readonly erreur = signal('');
  readonly aAnnuler = signal<Reservation | null>(null);
  readonly occupe = signal(false);
  readonly info = signal('');

  readonly libelles: Record<string, string> = {
    EN_ATTENTE: 'En attente',
    ACCEPTEE: 'Confirmée',
    REFUSEE: 'Refusée',
    ANNULEE: 'Annulée',
  };

  readonly compteurs = computed(() => {
    const rows = this.lignes();
    return Object.fromEntries(
      ONGLETS.map((o) => [o.cle, rows.filter((r) => o.statuts.includes(r.statut)).length]),
    ) as Record<Onglet, number>;
  });

  readonly visibles = computed(() => {
    const statuts = ONGLETS.find((o) => o.cle === this.onglet())!.statuts;
    const q = this.recherche().trim().toLowerCase();
    return this.lignes()
      .filter((r) => statuts.includes(r.statut))
      .filter((r) => {
        if (!q) return true;
        return `${this.titre(r)} ${this.guideNom(r)}`.toLowerCase().includes(q);
      })
      .sort((a, b) => b.id - a.id);
  });

  ngOnInit() {
    const notif = history.state?.notif;
    if (notif) this.info.set(notif);
    // Rechargement automatique toutes les 10 s : quand le guide confirme ou annule,
    // le statut change tout seul dans la liste du touriste.
    timer(0, 10000)
      .pipe(
        switchMap(() =>
          forkJoin({
            rows: this.reservations.lister(this.auth.email() || undefined),
            circuits: this.circuitsApi.list().pipe(catchError(() => of([] as Circuit[]))),
            guides: this.guidesApi.list().pipe(catchError(() => of([] as Guide[]))),
          }).pipe(
            catchError(() => {
              this.erreur.set('Impossible de charger vos réservations pour le moment.');
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((data) => {
        this.chargement.set(false);
        if (!data) return;
        this.erreur.set('');
        this.lignes.set(data.rows);
        this.circuits.set(new Map(data.circuits.map((c) => [c.id, c])));
        this.guides.set(new Map(data.guides.map((g) => [g.id, g])));
      });
  }

  titre(r: Reservation) {
    return this.circuits().get(r.circuitId)?.titre ?? `Circuit n° ${r.circuitId}`;
  }
  guideNom(r: Reservation) {
    return this.guides().get(r.guideId)?.nom ?? '';
  }
  photo(r: Reservation) {
    const c = this.circuits().get(r.circuitId);
    return circuitPhoto({ titre: c?.titre ?? '', ville: c?.ville ?? null, image: c?.image });
  }
  initiales() {
    return (
      this.auth
        .name()
        .split(' ')
        .map((p) => p[0])
        .join('')
        .slice(0, 2)
        .toUpperCase() || 'MO'
    );
  }
  peutAnnuler(r: Reservation) {
    return ['EN_ATTENTE', 'ACCEPTEE'].includes(r.statut);
  }

  annuler(r: Reservation) {
    this.occupe.set(true);
    this.reservations.annuler(r.id).subscribe({
      next: (maj) => {
        this.lignes.update((rows) => rows.map((x) => (x.id === r.id ? maj : x)));
        this.aAnnuler.set(null);
        this.occupe.set(false);
        this.info.set(`La réservation n° ${r.id} a été annulée.`);
      },
      error: () => {
        this.occupe.set(false);
        this.aAnnuler.set(null);
        this.erreur.set('Annulation impossible pour le moment.');
      },
    });
  }
}