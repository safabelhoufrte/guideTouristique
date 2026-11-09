import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { API } from '../pages/reservation/reservation.config';
import {
  CircuitResume,
  GuideResume,
  Reservation,
  ReservationRequest,
} from '../models/reservation';

// true  = circuits et guides simulés (services de Nada et Safa pas encore prêts)
const FAUX_CIRCUITS_GUIDES = true;
// false = vrai appel vers reservation-service (port 8083)
const FAUSSE_RESERVATION = false;

@Injectable({ providedIn: 'root' })
export class ReservationService {
  private http = inject(HttpClient);

  getCircuits(): Observable<CircuitResume[]> {
    if (FAUX_CIRCUITS_GUIDES) {
      return of([
        { id: 1, titre: 'Médina et Kasbah', guideId: 1, dureeHeures: 4, prix: 250 },
        { id: 2, titre: 'Vallée du Paradis', guideId: 1, dureeHeures: 6, prix: 400 },
      ]);
    }
    return this.http.get<CircuitResume[]>(API.circuits);
  }

  getGuide(id: number): Observable<GuideResume> {
    if (FAUX_CIRCUITS_GUIDES) {
      return of({ id, nom: 'Youssef Benali' });
    }
    return this.http.get<GuideResume>(`${API.guides}/${id}`);
  }

  creer(r: ReservationRequest): Observable<Reservation> {
    if (FAUSSE_RESERVATION) {
      return of({
        ...r,
        id: Date.now(),
        guideId: 1,
        prixTotal: 0,
        statut: 'EN_ATTENTE',
        dateCreation: new Date().toISOString(),
      });
    }
    return this.http.post<Reservation>(API.reservations, r);
  }

  lister(email?: string): Observable<Reservation[]> {
    let params = new HttpParams();
    if (email) params = params.set('email', email);
    return this.http.get<Reservation[]>(API.reservations, { params });
  }

  annuler(id: number): Observable<Reservation> {
    return this.http.patch<Reservation>(`${API.reservations}/${id}/annuler`, {});
  }
}