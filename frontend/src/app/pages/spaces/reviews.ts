import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, combineLatest, finalize, forkJoin, map, of, startWith, Subject, switchMap } from 'rxjs';
import { Avis, AvisMoyenne, extractAvisError } from '../../models/avis';
import { AvisService } from '../../services/avis.service';
import { AuthService } from '../../auth/auth.service';
import { WorkspaceService } from '../../services/workspace.service';
import { Reservation } from '../../models/reservation';
import { ReviewForm } from './review-form';

@Component({
  selector: 'app-reviews',
  imports: [DatePipe, DecimalPipe, RouterLink, ReviewForm],
  host: { class: 'workspace-ui' },
  styles: [
    `
      .reviews-public-grid {
        display: grid;
        grid-template-columns: 1.35fr 1fr;
        gap: 28px;
        align-items: start;
        margin-top: 20px;
      }
      @media (max-width: 900px) {
        .reviews-public-grid {
          grid-template-columns: 1fr;
        }
      }
      .review-card {
        padding: 24px;
      }
      .review-header {
        display: flex;
        justify-content: space-between;
        align-items: baseline;
        flex-wrap: wrap;
        gap: 8px;
        margin-bottom: 12px;
      }
      .review-author {
        font-size: 18px;
        font-weight: 700;
        color: var(--primary, #0e7490);
      }
      .review-date {
        font-size: 14px;
        color: var(--muted, #64748b);
      }
      .review-stars-group {
        display: flex;
        flex-wrap: wrap;
        gap: 16px;
        margin-bottom: 12px;
      }
      .stars-line {
        display: flex;
        align-items: center;
        gap: 6px;
      }
      .stars-label {
        font-size: 14px;
        font-weight: 600;
        color: var(--primary, #0e7490);
      }
      .stars {
        color: #f59e0b;
        font-size: 16px;
        letter-spacing: 1px;
      }
      .moyenne-card {
        margin-bottom: 24px;
        padding: 20px 24px;
      }
      .moyenne-card h3 {
        font-size: 18px;
        color: var(--primary, #0e7490);
        margin-bottom: 8px;
      }
      .moyenne-score {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;
      }
      .score-val {
        font-size: 26px;
        font-weight: 700;
        color: var(--primary, #0e7490);
      }
      .count {
        color: var(--muted, #64748b);
        font-size: 14px;
      }
      .login-box {
        padding: 24px;
      }
      .login-box p {
        color: var(--muted, #64748b);
        line-height: 1.6;
        margin-top: 8px;
      }
      .reservation-panel {
        padding: 24px;
      }
      .reservation-panel p {
        color: var(--muted, #64748b);
        margin: 8px 0 16px;
      }
      .reservation-panel select,
      .reservation-panel input {
        width: 100%;
        min-height: 44px;
        padding: 11px 14px;
        border: 1px solid var(--panel-border, #d5e4e8);
        border-radius: 10px;
        font: inherit;
        color: #3a6ea5;
      }
      .manual-id-block {
        margin-top: 20px;
        padding-top: 18px;
        border-top: 1px solid var(--panel-border, #d5e4e8);
      }
      .manual-id-block label {
        margin-bottom: 8px;
      }
    `,
  ],
  template: `
    <div
      [class.workspace]="scope === 'public'"
      [style.padding-top.px]="scope === 'public' ? 32 : null"
    >
      <header class="section-header">
        <div>
          <h2>
            {{
              scope === 'admin'
                ? 'Modérer les avis'
                : scope === 'me'
                  ? 'Mes avis'
                  : 'Avis des voyageurs'
            }}
          </h2>
          <p>
            {{
              scope === 'admin'
                ? 'Publiez ou masquez les contributions.'
                : 'Des souvenirs et des retours sur les visites.'
            }}
          </p>
        </div>
        <button class="button secondary" type="button" (click)="refresh.next()">Actualiser</button>
      </header>

      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      }

      @if (scope === 'public') {
        <div class="reviews-public-grid">
          <!-- Colonne gauche : avis et moyenne -->
          <div class="reviews-col-left">
            @if (hasFilter() && vm().moyenne; as m) {
              <div class="moyenne-card surface">
                <h3>Note globale</h3>
                @if (m.moyenne !== null) {
                  <div class="moyenne-score">
                    <span class="score-val">{{ m.moyenne | number: '1.1-1' }} / 5</span>
                    <span class="stars" [attr.aria-label]="m.moyenne + ' sur 5'">
                      <span aria-hidden="true">{{ getStars(m.moyenne) }}</span>
                    </span>
                    <span class="count">({{ m.nombreAvis }} avis)</span>
                  </div>
                } @else {
                  <p class="muted">Aucune note pour le moment ({{ m.nombreAvis }} avis)</p>
                }
              </div>
            }

            @if (vm().loading) {
              <p class="surface" role="status">Chargement des avis…</p>
            } @else if (vm().error) {
              <p class="error" role="alert">{{ vm().error }}</p>
            } @else if (!vm().rows.length) {
              <section class="surface empty">
                <h3>Aucun avis pour le moment</h3>
                <p>Soyez le premier à partager votre expérience !</p>
              </section>
            } @else {
              <div class="stack">
                @for (a of vm().rows; track a.id) {
                  <article class="surface review-card">
                    <div class="review-header">
                      <strong class="review-author">{{ a.auteur }}</strong>
                      <span class="review-date">{{ a.dateCreation | date: 'dd/MM/yyyy' }}</span>
                    </div>
                    <div class="review-stars-group">
                      <div class="stars-line">
                        <span class="stars-label">Guide :</span>
                        <span class="stars" [attr.aria-label]="a.noteGuide + ' sur 5'">
                          <span aria-hidden="true">{{ getStars(a.noteGuide) }}</span>
                        </span>
                      </div>
                      <div class="stars-line">
                        <span class="stars-label">Circuit :</span>
                        <span class="stars" [attr.aria-label]="a.noteCircuit + ' sur 5'">
                          <span aria-hidden="true">{{ getStars(a.noteCircuit) }}</span>
                        </span>
                      </div>
                    </div>
                    <p class="review-text">{{ a.commentaire }}</p>
                  </article>
                }
              </div>
            }
          </div>

          <!-- Colonne droite : Donner mon avis -->
          <aside class="reviews-col-right">
            @if (!auth.has('USER')) {
              <div class="surface login-box">
                <h3 class="panel-heading">Donner mon avis</h3>
                <p>Vous avez participé à l'une de nos visites ? Connectez-vous avec votre compte voyageur pour publier votre avis.</p>
                <div style="margin-top: 18px;">
                  <button class="button primary" style="width: 100%;" (click)="login()">
                    Se connecter
                  </button>
                </div>
              </div>
            } @else {
              @if (activeReservationId(); as resId) {
                <app-review-form
                  [reservationId]="resId"
                  [embedded]="true"
                  (reviewCreated)="onReviewCreated()"
                ></app-review-form>
                <div style="margin-top: 12px; text-align: center;">
                  <button
                    class="button secondary"
                    style="width: 100%;"
                    type="button"
                    (click)="resetReservationSelection()"
                  >
                    Choisir une autre réservation
                  </button>
                </div>
              } @else {
                <div class="surface reservation-panel">
                  <h3 class="panel-heading">Donner mon avis</h3>
                  @if (loadingReservations()) {
                    <p class="muted">Recherche de vos réservations…</p>
                  } @else if (userReservations().length > 0) {
                    <p>Sélectionnez la réservation concernée par votre visite :</p>
                    <div style="margin-bottom: 16px;">
                      <label for="reservation-select">Réservation terminée</label>
                      <select id="reservation-select" (change)="onSelectReservation($event)">
                        <option value="">-- Choisir une réservation --</option>
                        @for (r of userReservations(); track r.id) {
                          <option [value]="r.id">
                            Réservation n° {{ r.id }} (Circuit n° {{ r.circuitId }})
                          </option>
                        }
                      </select>
                    </div>
                  } @else {
                    <p>Aucune réservation n'a été trouvée pour votre compte.</p>
                  }

                  <div class="manual-id-block">
                    <label for="manual-res-id">Ou indiquez directement votre n° de réservation :</label>
                    <div style="display: flex; gap: 8px; margin-top: 6px;">
                      <input
                        id="manual-res-id"
                        type="number"
                        min="1"
                        #manualInput
                        placeholder="Ex: 25"
                      />
                      <button
                        class="button secondary"
                        type="button"
                        (click)="setManualReservation(manualInput.value)"
                      >
                        Valider
                      </button>
                    </div>
                  </div>
                </div>
              }
            }
          </aside>
        </div>
      } @else {
        <!-- Mode 'me' ou 'admin' -->
        @if (vm().loading) {
          <p class="surface" role="status">Chargement des avis…</p>
        } @else if (vm().error) {
          <p class="error" role="alert">{{ vm().error }}</p>
        } @else if (!vm().rows.length) {
          <section class="surface empty">
            <h3>Aucun avis pour le moment</h3>
            <p>Les avis sont possibles après une visite acceptée.</p>
            @if (scope === 'me') {
              <a class="button primary" routerLink="/espace-touriste">Voir mes réservations</a>
            }
          </section>
        } @else {
          <div class="stack">
            @for (a of vm().rows; track a.id) {
              <article class="surface review-card">
                <div class="booking-heading">
                  <strong class="review-author">{{ a.auteur }}</strong>
                  <span class="badge">{{ a.publie ? 'Publié' : (scope === 'me' ? 'En attente' : 'Masqué') }}</span>
                </div>
                <div class="review-stars-group">
                  <div class="stars-line">
                    <span class="stars-label">Guide :</span>
                    <span class="stars" [attr.aria-label]="a.noteGuide + ' sur 5'">
                      <span aria-hidden="true">{{ getStars(a.noteGuide) }}</span>
                    </span>
                  </div>
                  <div class="stars-line">
                    <span class="stars-label">Circuit :</span>
                    <span class="stars" [attr.aria-label]="a.noteCircuit + ' sur 5'">
                      <span aria-hidden="true">{{ getStars(a.noteCircuit) }}</span>
                    </span>
                  </div>
                </div>
                <p class="review-text">{{ a.commentaire }}</p>
                <p class="muted">
                  Circuit n° {{ a.circuitId }} · {{ a.dateCreation | date: 'dd/MM/yyyy' }}
                </p>
                @if (scope === 'admin') {
                  <div class="actions">
                    <button class="button secondary" [disabled]="busy()" (click)="toggle(a)">
                      {{ a.publie ? 'Masquer cet avis' : 'Publier cet avis' }}
                    </button>
                  </div>
                }
              </article>
            }
          </div>
        }
      }
    </div>
  `,
})
export class Reviews {
  private readonly api = inject(AvisService);
  private readonly workspaceService = inject(WorkspaceService);
  readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);

  readonly scope = (this.route.snapshot.data['scope'] || 'public') as
    'public' | 'me' | 'admin';

  readonly refresh = new Subject<void>();
  readonly error = signal('');
  readonly busy = signal(false);

  readonly activeReservationId = signal<number | null>(null);
  readonly userReservations = signal<Reservation[]>([]);
  readonly loadingReservations = signal(false);

  readonly vm = toSignal(
    combineLatest([
      this.refresh.pipe(startWith(undefined)),
      this.route.queryParamMap,
    ]).pipe(
      switchMap(([_, qp]) => {
        const circuitId = qp.get('circuitId') ? Number(qp.get('circuitId')) : undefined;
        const guideId = qp.get('guideId') ? Number(qp.get('guideId')) : undefined;
        const filters = (circuitId || guideId) ? { circuitId, guideId } : undefined;

        const avis$ = this.api.getAvis(this.scope, filters);
        const moyenne$ = (this.scope === 'public' && (circuitId || guideId))
          ? this.api.getMoyenne({ circuitId, guideId }).pipe(
              catchError(() => of(null))
            )
          : of(null);

        return forkJoin({ rows: avis$, moyenne: moyenne$ }).pipe(
          map(({ rows, moyenne }) => ({ loading: false, rows, moyenne, error: '' })),
          catchError((e) =>
            of({
              loading: false,
              rows: [] as Avis[],
              moyenne: null as AvisMoyenne | null,
              error: extractAvisError(e),
            }),
          ),
          startWith({
            loading: true,
            rows: [] as Avis[],
            moyenne: null as AvisMoyenne | null,
            error: '',
          }),
        );
      }),
    ),
    {
      initialValue: {
        loading: true,
        rows: [] as Avis[],
        moyenne: null as AvisMoyenne | null,
        error: '',
      },
    },
  );

  constructor() {
    const qpRes = this.route.snapshot.queryParamMap.get('reservationId');
    if (qpRes) {
      const num = Number(qpRes);
      if (Number.isSafeInteger(num) && num > 0) {
        this.activeReservationId.set(num);
      }
    } else if (this.scope === 'public' && this.auth.has('USER')) {
      this.loadUserReservations();
    }
  }

  hasFilter(): boolean {
    const qp = this.route.snapshot.queryParamMap;
    return qp.has('circuitId') || qp.has('guideId');
  }

  getStars(note: number): string {
    const n = Math.max(0, Math.min(5, Math.round(note)));
    return '★'.repeat(n) + '☆'.repeat(5 - n);
  }

  login() {
    this.auth.login('/avis');
  }

  loadUserReservations() {
    this.loadingReservations.set(true);
    this.workspaceService
      .reservations('me')
      .pipe(finalize(() => this.loadingReservations.set(false)))
      .subscribe({
        next: (list) => {
          this.userReservations.set(list || []);
        },
        error: () => this.userReservations.set([]),
      });
  }

  onSelectReservation(event: Event) {
    const val = Number((event.target as HTMLSelectElement).value);
    if (Number.isSafeInteger(val) && val > 0) {
      this.activeReservationId.set(val);
    }
  }

  setManualReservation(val: string) {
    const num = Number(val);
    if (Number.isSafeInteger(num) && num > 0) {
      this.activeReservationId.set(num);
    }
  }

  resetReservationSelection() {
    this.activeReservationId.set(null);
    if (this.userReservations().length === 0) {
      this.loadUserReservations();
    }
  }

  onReviewCreated() {
    this.refresh.next();
  }

  toggle(a: Avis) {
    if (this.busy()) return;
    this.busy.set(true);
    this.api
      .publishAvis(a.id, !a.publie)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: () => this.refresh.next(),
        error: (e) => this.error.set(extractAvisError(e)),
      });
  }
}
