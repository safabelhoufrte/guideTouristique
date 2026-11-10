import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Circuit, CircuitFilters } from '../models/circuit';

@Injectable({ providedIn: 'root' })
export class CircuitService {
  private readonly http = inject(HttpClient);

  list(filters: CircuitFilters = {}) {
    let params = new HttpParams();
    if (filters.ville?.trim()) params = params.set('ville', filters.ville.trim());
    if (filters.guideId) params = params.set('guideId', filters.guideId);
    return this.http.get<Circuit[]>('/api/circuits', { params, timeout: 5000 });
  }

  get(id: number) {
    return this.http.get<Circuit>(`/api/circuits/${id}`, { timeout: 5000 });
  }
}
