import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { catchError, forkJoin, merge, of, Subject, switchMap, timer } from 'rxjs';
import { Circuit, Guide } from '../../models/circuit';
import { Reservation } from '../../models/reservation';
import { CircuitService } from '../../services/circuit.service';
import { GuideService } from '../../services/guide.service';
import { WorkspaceService, apiError } from '../../services/workspace.service';

type Onglet = 'confirmees' | 'attente' | 'annulees';

const ONGLETS: { cle: Onglet; label: string; statuts: string[] }[] = [
  { cle: 'confirmees', label: 'Confirmées', statuts: ['ACCEPTEE'] },
  { cle: 'attente', label: 'En attente', statuts: ['EN_ATTENTE'] },
  { cle: 'annulees', label: 'Annulées', statuts: ['ANNULEE', 'REFUSEE'] },
];

@Component({
  selector: 'app-admin-reservations',
  imports: [CurrencyPipe, DatePipe, FormsModule],
  host: { class: 'workspace-ui' },
  template: `<header class="section-header">
      <div>
        <h2>Réservations des touristes</h2>
        <p>Confirmées, en attente ou annulées. Mise à jour automatique toutes les 15 secondes.</p>
      </div>
      <button class="button secondary" (click)="actualiser()">Actualiser</button>
    </header>
    @if (message()) {
      <p class="success" role="status">{{ message() }}</p>
    }
    @if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
    }
    <div class="stats">
      <div class="surface stat">
        <strong>{{ compteurs()['confirmees'] }}</strong>
        <p>Confirmées</p>
      </div>
      <div class="surface stat">
        <strong>{{ compteurs()['attente'] }}</strong>
        <p>En attente</p>
      </div>
      <div class="surface stat">
        <strong>{{ caConfirme() | currency: 'MAD' : 'code' : '1.0-0' }}</strong>
        <p>Montant des réservations confirmées</p>
      </div>
    </div>
    <div class="toolbar">
      <div class="tabs" role="tablist">
        @for (o of onglets; track o.cle) {
          <button
            type="button"
            role="tab"
            [class.active]="onglet() === o.cle"
            [attr.aria-selected]="onglet() === o.cle"
            (click)="onglet.set(o.cle)"
          >
            {{ o.label }} <span class="count">{{ compteurs()[o.cle] }}</span>
          </button>
        }
      </div>
      <input
        class="search"
        type="search"
        placeholder="Rechercher un touriste, un email ou un circuit"
        [ngModel]="recherche()"
        (ngModelChange)="recherche.set($event)"
      />
    </div>
    @if (chargement()) {
      <p class="surface" role="status">Chargement des réservations…</p>
    } @else if (!visibles().length) {
      <section class="surface empty"><h3>Aucune réservation dans cette catégorie</h3></section>
    } @else {
      <div class="surface table-wrap">
        <table>
          <thead>
            <tr>
              <th>N°</th>
              <th>Touriste</th>
              <th>Circuit</th>
              <th>Date de visite</th>
              <th>Pers.</th>
              <th>Total</th>
              <th>Statut</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            @for (r of visibles(); track r.id) {
              <tr>
                <td>{{ r.id }}</td>
                <td>
                  <strong>{{ r.nom }}</strong
                  ><small>{{ r.email }}</small>
                </td>
                <td>
                  <strong>{{ titre(r) }}</strong>
                  @if (guideNom(r)) {
                    <small>Guide : {{ guideNom(r) }}</small>
                  }
                </td>
                <td>{{ r.dateVisite | date: 'dd/MM/yyyy' }}</td>
                <td>{{ r.nbPersonnes }}</td>
                <td>
                  <strong>{{ r.prixTotal | currency: 'MAD' : 'code' : '1.0-0' }}</strong>
                  @if (r.codePromo) {
                    <small>Code {{ r.codePromo }} (-{{ r.reductionPct }} %)</small>
                  }
                </td>
                <td>
                  <span class="badge" [attr.data-status]="r.statut">{{ labels[r.statut] }}</span>
                </td>
                <td class="table-actions">
                  @if (r.statut === 'EN_ATTENTE') {
                    <button class="button primary" [disabled]="busy()" (click)="act(r, 'accepter')">
                      Accepter
                    </button>
                    <button class="button secondary" [disabled]="busy()" (click)="act(r, 'refuser')">
                      Refuser
                    </button>
                  }
                  @if (r.statut === 'EN_ATTENTE' || r.statut === 'ACCEPTEE') {
                    <button class="button danger" [disabled]="busy()" (click)="act(r, 'annuler')">
                      Annuler
                    </button>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }`,
  styles: `
    .toolbar { display: flex; justify-content: space-between; align-items: center; gap: 16px; flex-wrap: wrap; margin: 22px 0 16px; }
    .tabs { display: flex; gap: 22px; }
    .tabs button { background: none; border: 0; border-bottom: 3px solid transparent; padding: 10px 0; font-size: 16px; color: #64748b; cursor: pointer; display: flex; gap: 8px; align-items: center; }
    .tabs button.active { color: #0e7490; border-bottom-color: #0e7490; font-weight: 600; }
    .count { background: #e2e8f0; border-radius: 999px; padding: 1px 9px; font-size: 13px; }
    .active .count { background: #0e7490; color: #fff; }
    .search { width: 340px; max-width: 100%; padding: 10px 14px; border: 1px solid #cbd5e1; border-radius: 10px; font-size: 15px; }
  `,
})
export class AdminReservations {
  private readonly api = inject(WorkspaceService);
  private readonly circuitApi = inject(CircuitService);
  private readonly guideApi = inject(GuideService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly refresh = new Subject<void>();

  readonly onglets = ONGLETS;
  readonly onglet = signal<Onglet>('confirmees');
  readonly recherche = signal('');
  readonly lignes = signal<Reservation[]>([]);
  readonly circuits = signal<Map<number, Circuit>>(new Map());
  readonly guides = signal<Map<number, Guide>>(new Map());
  readonly chargement = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  readonly labels: Record<string, string> = {
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
  readonly caConfirme = computed(() =>
    this.lignes()
      .filter((r) => r.statut === 'ACCEPTEE')
      .reduce((somme, r) => somme + Number(r.prixTotal), 0),
  );
  readonly visibles = computed(() => {
    const statuts = ONGLETS.find((o) => o.cle === this.onglet())!.statuts;
    const q = this.recherche().trim().toLowerCase();
    return this.lignes()
      .filter((r) => statuts.includes(r.statut))
      .filter(
        (r) =>
          !q || `${r.nom} ${r.email} ${this.titre(r)} ${this.guideNom(r)}`.toLowerCase().includes(q),
      )
      .sort((a, b) => b.id - a.id);
  });

  constructor() {
    merge(timer(0, 15000), this.refresh)
      .pipe(
        switchMap(() =>
          forkJoin({
            rows: this.api.reservations('admin'),
            circuits: this.circuitApi.list().pipe(catchError(() => of([] as Circuit[]))),
            guides: this.guideApi.list().pipe(catchError(() => of([] as Guide[]))),
          }).pipe(
            catchError((e) => {
              this.error.set(apiError(e));
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((data) => {
        this.chargement.set(false);
        if (!data) return;
        this.error.set('');
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
  actualiser() {
    this.refresh.next();
  }

  act(r: Reservation, action: 'annuler' | 'accepter' | 'refuser') {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.api.decision(r.id, action).subscribe({
      next: (maj) => {
        this.lignes.update((rows) => rows.map((x) => (x.id === maj.id ? maj : x)));
        this.message.set('Le statut de la réservation n° ' + r.id + ' a été mis à jour.');
        this.busy.set(false);
      },
      error: (e) => {
        this.error.set(apiError(e));
        this.busy.set(false);
      },
    });
  }
}