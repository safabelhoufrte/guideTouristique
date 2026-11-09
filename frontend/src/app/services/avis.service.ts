import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Avis, AvisMoyenne, CreateAvisDto, Eligibility } from '../models/avis';

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

  checkEligibility(reservationId: number) {
    return this.http.get<Eligibility>(`/api/reservations/${reservationId}/eligibilite`, {
      timeout: 8000,
    });
  }
}
