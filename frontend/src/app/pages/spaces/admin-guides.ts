import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, map, of, startWith, Subject, switchMap, finalize } from 'rxjs';
import { GuideProfile, WorkspaceService, apiError } from '../../services/workspace.service';
@Component({
  selector: 'app-admin-guides',
  imports: [RouterLink, CurrencyPipe],
  host: { class: 'workspace-ui' },
  template: ` <header class="section-header">
      <div>
        <h2>Gérer les guides</h2>
        <p>Profils publics et comptes associés.</p>
      </div>
      <a class="button primary" routerLink="nouveau">Créer un guide</a>
    </header>
    @if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
    }
    @if (pending(); as g) {
      <section class="confirm-box">
        <h3>Archiver {{ g.nom }} ?</h3>
        <p>
          Son profil sera retiré de l’annuaire et les nouvelles réservations seront refusées.
          Réaffectez ses circuits à un autre guide.
        </p>
        <div class="actions">
          <button class="button danger" [disabled]="busy()" (click)="archive(g)">Archiver</button
          ><button class="button secondary" (click)="pending.set(null)">Conserver</button>
        </div>
      </section>
    }
    @if (vm().loading) {
      <p class="surface" role="status">Chargement…</p>
    } @else if (vm().error) {
      <p class="error" role="alert">{{ vm().error }}</p>
    } @else if (!vm().rows.length) {
      <section class="surface empty">
        <h3>Aucun guide</h3>
        <a class="button primary" routerLink="nouveau">Créer un guide</a>
      </section>
    } @else {
      <div class="surface table-wrap">
        <table>
          <thead>
            <tr>
              <th>Guide</th>
              <th>Disponibilité</th>
              <th>Tarif / jour</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            @for (g of vm().rows; track g.id) {
              <tr>
                <td>
                  <strong>{{ g.nom }}</strong
                  ><small>{{ g.ville }} · {{ g.langues?.join(', ') }}</small
                  ><small>{{ g.ownerSubject ? 'Compte lié' : 'Compte non associé' }}</small>
                </td>
                <td>{{ g.disponible ? 'Disponible' : 'Indisponible' }}</td>
                <td>{{ g.tarifParJour | currency: 'MAD' : 'code' }}</td>
                <td>
                  <div class="table-actions">
                    <a class="button secondary" [routerLink]="[g.id]">Modifier</a
                    ><button class="button danger" (click)="pending.set(g)">Archiver</button>
                  </div>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }`,
})
export class AdminGuides {
  private readonly api = inject(WorkspaceService);
  readonly refresh = new Subject<void>();
  readonly pending = signal<GuideProfile | null>(null);
  readonly error = signal('');
  readonly busy = signal(false);
  readonly vm = toSignal(
    this.refresh.pipe(
      startWith(undefined),
      switchMap(() =>
        this.api.adminGuides().pipe(
          map((rows) => ({ loading: false, rows, error: '' })),
          catchError((e) => of({ loading: false, rows: [] as GuideProfile[], error: apiError(e) })),
          startWith({ loading: true, rows: [] as GuideProfile[], error: '' }),
        ),
      ),
    ),
    { initialValue: { loading: true, rows: [] as GuideProfile[], error: '' } },
  );
  archive(g: GuideProfile) {
    if (this.busy()) return;
    this.busy.set(true);
    this.api
      .archiveGuide(g.id)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: () => {
          this.pending.set(null);
          this.refresh.next();
        },
        error: (e) => this.error.set(apiError(e)),
      });
  }
}
