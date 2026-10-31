import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Guide } from '../models/circuit';

@Injectable({ providedIn: 'root' })
export class GuideService {
  private readonly http = inject(HttpClient);

  list() {
    return this.http.get<Guide[]>('/api/guides', { timeout: 5000 });
  }

  get(id: number) {
    return this.http.get<Guide>(`/api/guides/${id}`, { timeout: 5000 });
  }
}
