import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, map, of, startWith, Subject, switchMap, finalize } from 'rxjs';
import { Reservation } from '../../models/reservation';
import { WorkspaceService, apiError } from '../../services/workspace.service';

@Component({
  selector: 'app-space-reservations',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  host: { class: 'workspace-ui' },
  template: `<header class="section-header">
      <div>
        <h2>
          {{
            scope === 'admin'
              ? 'Gérer les réservations'
              : scope === 'guide'
                ? 'Mes visiteurs'
                : 'Mes réservations'
          }}
        </h2>
        <p>
          {{
            scope === 'guide'
              ? 'Les réservations des circuits qui vous sont confiés.'
              : 'Suivez les demandes et leur statut.'
          }}
        </p>
      </div>
      <button class="button secondary" (click)="reload()">Actualiser</button>
    </header>
    @if (message()) {
      <p class="success" role="status">{{ message() }}</p>
    }
    @if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
    }
    @if (pending(); as request) {
      <section class="confirm-box">
        <h3>Annuler la réservation n° {{ request.id }} ?</h3>
        <p>La demande passera au statut Annulée.</p>
        <div class="actions">
          <button class="button danger" [disabled]="busy()" (click)="act(request, 'annuler')">
            Confirmer l’annulation</button
          ><button class="button secondary" [disabled]="busy()" (click)="pending.set(null)">
            Conserver
          </button>
        </div>
      </section>
    }
    @if (vm().loading) {
      <p class="surface" role="status">Chargement des réservations…</p>
    } @else if (vm().error) {
      <section class="surface empty" role="alert">
        <p>{{ vm().error }}</p>
        <button class="button primary" (click)="reload()">Réessayer</button>
      </section>
    } @else {
      <div class="stats">
        <div class="surface stat">
          <strong>{{ vm().rows.length }}</strong>
          <p>Réservations</p>
        </div>
        <div class="surface stat">
          <strong>{{ waiting() }}</strong>
          <p>En attente</p>
        </div>
        <div class="surface stat">
          <strong>{{ accepted() }}</strong>
          <p>Acceptées</p>
        </div>
      </div>
      @if (!vm().rows.length) {
        <section class="surface empty">
          <h3>Aucune réservation pour le moment</h3>
          <p>
            {{
              scope === 'me'
                ? 'Votre prochaine découverte commence ici.'
                : 'Les nouvelles demandes apparaîtront ici.'
            }}
          </p>
          @if (scope === 'me') {
            <a class="button primary" routerLink="/circuits">Découvrir les circuits</a>
          }
        </section>
      }
      <div class="stack" style="margin-top:22px">
        @for (r of vm().rows; track r.id) {
          <article class="surface">
            <div class="booking-heading">
              <div>
                <h3>Réservation n° {{ r.id }}</h3>
                <a [routerLink]="['/circuits', r.circuitId]"
                  >Voir le circuit n° {{ r.circuitId }}</a
                >
              </div>
              <span class="badge" [attr.data-status]="r.statut">{{ labels[r.statut] }}</span>
            </div>
            <div class="facts">
              <div>
                <span>Date de visite</span><strong>{{ r.dateVisite | date: 'dd/MM/yyyy' }}</strong>
              </div>
              <div>
                <span>Participants</span><strong>{{ r.nbPersonnes }}</strong>
              </div>
              <div>
                <span>Montant</span><strong>{{ r.prixTotal | currency: 'MAD' : 'code' }}</strong>
              </div>
              @if (scope !== 'me') {
                <div>
                  <span>Voyageur</span><strong>{{ r.nom }}</strong>
                </div>
              }
            </div>
            <div class="actions">
              @if (scope === 'admin' && r.statut === 'EN_ATTENTE') {
                <button class="button primary" [disabled]="busy()" (click)="act(r, 'accepter')">
                  Accepter</button
                ><button class="button secondary" [disabled]="busy()" (click)="act(r, 'refuser')">
                  Refuser
                </button>
              }
              @if (scope !== 'guide' && canCancel(r)) {
                <button class="button danger" [disabled]="busy()" (click)="pending.set(r)">
                  Annuler
                </button>
              }
              @if (scope === 'me' && canReview(r)) {
                <a class="button primary" [routerLink]="['/espace-touriste/avis/nouveau', r.id]"
                  >Donner mon avis</a
                >
              }
            </div>
          </article>
        }
      </div>
    }`,
})
export class SpaceReservations {
  private readonly api = inject(WorkspaceService);
  readonly scope = inject(ActivatedRoute).snapshot.data['scope'] as 'me' | 'guide' | 'admin';
  private readonly refresh = new Subject<void>();
  readonly busy = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  readonly pending = signal<Reservation | null>(null);
  readonly labels = {
    EN_ATTENTE: 'En attente',
    ACCEPTEE: 'Acceptée',
    REFUSEE: 'Refusée',
    ANNULEE: 'Annulée',
  };
  readonly vm = toSignal(
    this.refresh.pipe(
      startWith(undefined),
      switchMap(() =>
        this.api.reservations(this.scope).pipe(
          map((rows) => ({ loading: false, rows, error: '' })),
          catchError((e) => of({ loading: false, rows: [] as Reservation[], error: apiError(e) })),
          startWith({ loading: true, rows: [] as Reservation[], error: '' }),
        ),
      ),
    ),
    { initialValue: { loading: true, rows: [] as Reservation[], error: '' } },
  );
  readonly waiting = computed(() => this.vm().rows.filter((r) => r.statut === 'EN_ATTENTE').length);
  readonly accepted = computed(() => this.vm().rows.filter((r) => r.statut === 'ACCEPTEE').length);
  canCancel(r: Reservation) {
    return (
      ['EN_ATTENTE', 'ACCEPTEE'].includes(r.statut) &&
      r.dateVisite >= new Date().toLocaleDateString('en-CA')
    );
  }
  canReview(r: Reservation) {
    return r.statut === 'ACCEPTEE' && r.dateVisite < new Date().toLocaleDateString('en-CA');
  }
  reload() {
    this.refresh.next();
  }
  act(r: Reservation, action: 'annuler' | 'accepter' | 'refuser') {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.message.set('');
    this.api
      .decision(r.id, action)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: () => {
          this.pending.set(null);
          this.message.set('Le statut a été mis à jour.');
          this.reload();
        },
        error: (e) => this.error.set(apiError(e)),
      });
  }
}
