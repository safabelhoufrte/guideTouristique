import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, forkJoin, map, of } from 'rxjs';
import { CircuitService } from '../../services/circuit.service';
import { WorkspaceService } from '../../services/workspace.service';
@Component({
  selector: 'app-admin-home',
  imports: [RouterLink],
  host: { class: 'workspace-ui' },
  template: ` <header class="section-header">
      <div>
        <h2>Vue d’ensemble</h2>
        <p>Les actions essentielles pour faire vivre votre plateforme.</p>
      </div>
    </header>
    <div class="stats">
      @for (item of vm(); track item.label) {
        <a class="surface stat" [routerLink]="item.path" style="text-decoration:none"
          ><strong>{{ item.value }}</strong>
          <p>{{ item.label }}</p></a
        >
      }
    </div>
    <section class="surface next-action">
      <h3 class="panel-heading">Votre prochaine action</h3>
      <p class="muted">Vérifiez les nouvelles demandes et préparez les prochaines découvertes.</p>
      <div class="actions">
        <a class="button primary" routerLink="/administration/reservations"
          >Traiter les réservations</a
        ><a class="button secondary" routerLink="/administration/circuits/nouveau"
          >Créer un circuit</a
        ><a class="button secondary" routerLink="/administration/guides/nouveau"
          >Ajouter un guide</a
        >
      </div>
    </section>
    <p class="hint">
      Un tiret indique qu’un service n’a pas pu répondre. Les autres espaces restent accessibles.
    </p>`,
})
export class AdminHome {
  private readonly api = inject(WorkspaceService);
  private readonly circuits = inject(CircuitService);
  readonly vm = toSignal(
    forkJoin({
      circuits: this.circuits.list().pipe(
        map((rows) => String(rows.length)),
        catchError(() => of('—')),
      ),
      guides: this.api.adminGuides().pipe(
        map((rows) => String(rows.length)),
        catchError(() => of('—')),
      ),
      reservations: this.api.reservations('admin').pipe(
        map((rows) => String(rows.filter((r) => r.statut === 'EN_ATTENTE').length)),
        catchError(() => of('—')),
      ),
    }).pipe(
      map((r) => [
        { label: 'Circuits au catalogue', value: r.circuits, path: '/administration/circuits' },
        { label: 'Guides actifs', value: r.guides, path: '/administration/guides' },
        {
          label: 'Demandes en attente',
          value: r.reservations,
          path: '/administration/reservations',
        },
      ]),
    ),
    {
      initialValue: [
        { label: 'Circuits au catalogue', value: '…', path: '/administration/circuits' },
        { label: 'Guides actifs', value: '…', path: '/administration/guides' },
        { label: 'Demandes en attente', value: '…', path: '/administration/reservations' },
      ],
    },
  );
}
