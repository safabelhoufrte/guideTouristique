import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { Avis, AvisMoyenne, CreateAvisDto, Eligibility, UserEligibility } from '../models/avis';

@Injectable({ providedIn: 'root' })
export class AvisService {
  private readonly http = inject(HttpClient);

  getAvis(scope: 'public' | 'me' | 'admin', filters?: { circuitId?: number; guideId?: number }) {
    if (scope !== 'public') {
      return this.http.get<Avis[]>(`/api/avis/${scope}`, { timeout: 8000 });
    }

    let params = new HttpParams();
    if (filters?.circuitId !== undefined && filters.circuitId !== null) {
      params = params.set('circuitId', filters.circuitId.toString());
    }
    if (filters?.guideId !== undefined && filters.guideId !== null) {
      params = params.set('guideId', filters.guideId.toString());
    }

    return this.http.get<Avis[]>('/api/avis', { params, timeout: 8000 });
  }

  getMoyenne(filters: { circuitId?: number; guideId?: number }) {
    let params = new HttpParams();
    if (filters.circuitId !== undefined && filters.circuitId !== null) {
      params = params.set('circuitId', filters.circuitId.toString());
    }
    if (filters.guideId !== undefined && filters.guideId !== null) {
      params = params.set('guideId', filters.guideId.toString());
    }
    return this.http.get<AvisMoyenne>('/api/avis/moyenne', { params, timeout: 8000 });
  }

  createAvis(body: CreateAvisDto) {
    return this.http.post<Avis>('/api/avis', body, { timeout: 10000 });
  }

  publishAvis(id: number, publie: boolean) {
    return this.http.patch<Avis>(`/api/avis/${id}/publication`, { publie }, { timeout: 8000 });
  }

  /** Eligibility of one reservation, computed by the Avis service. */
  checkEligibility(reservationId: number): Observable<Eligibility> {
    const params = new HttpParams().set('reservationId', reservationId.toString());
    return this.http.get<UserEligibility>('/api/avis/eligibilite', { params, timeout: 8000 }).pipe(
      map((u) => {
        const r = u.reservations.find((x) => x.reservationId === reservationId);
        return u.eligible && r
          ? { ...r, eligible: true, raison: '' }
          : {
              reservationId,
              circuitId: 0,
              guideId: 0,
              eligible: false,
              raison: 'Un avis est possible une seule fois, après la date d’une visite acceptée.',
            };
      }),
    );
  }

  getUserEligibility(reservationId?: number) {
    let params = new HttpParams();
    if (reservationId !== undefined && reservationId !== null) {
      params = params.set('reservationId', reservationId.toString());
    }
    return this.http.get<UserEligibility>('/api/avis/eligibilite', { params, timeout: 8000 });
  }
}
