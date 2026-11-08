import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Guide } from '../models/circuit';

@Injectable({ providedIn: 'root' })
export class GuideService {
  private readonly http = inject(HttpClient);

  list(filters: { ville?: string; langue?: string } = {}) {
    let params = new HttpParams();
    if (filters.ville?.trim()) params = params.set('ville', filters.ville.trim());
    if (filters.langue?.trim()) params = params.set('langue', filters.langue.trim());
    return this.http.get<Guide[]>('/api/guides', { params, timeout: 5000 });
  }

  get(id: number) {
    return this.http.get<Guide>(`/api/guides/${id}`, { timeout: 5000 });
  }
}
