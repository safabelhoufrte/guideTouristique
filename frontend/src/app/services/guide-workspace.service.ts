import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Circuit } from '../models/circuit';
import { Avis } from './workspace.service';

export interface GuideProfile {
  id: number;
  nom: string;
  prenom: string | null;
  ville: string | null;
  langues: string[];
  email: string | null;
  telephone: string | null;
  tarifParJour: number;
  disponible: boolean;
  noteMoyenne?: number | null;
}
export interface GuideProfileUpdate {
  nom: string;
  prenom: string | null;
  ville: string | null;
  langues: string[];
  email: string | null;
  telephone: string | null;
  tarifParJour: number;
}

@Injectable({ providedIn: 'root' })
export class GuideWorkspaceService {
  private http = inject(HttpClient);
  me(): Observable<GuideProfile> {
    return this.http.get<GuideProfile>('/api/guides/me', { timeout: 12000 });
  }
  updateMe(body: GuideProfileUpdate): Observable<GuideProfile> {
    return this.http.put<GuideProfile>('/api/guides/me', body, { timeout: 12000 });
  }
  circuits(guideId: number): Observable<Circuit[]> {
    return this.http.get<Circuit[]>('/api/circuits', {
      params: new HttpParams().set('guideId', guideId), timeout: 12000
    });
  }
  // Consultation uniquement : il faut une route sécurisée « mes réservations »
  // côté service Réservation avant de pouvoir afficher des demandes privées.
  // Même exigence pour les avis privés ; les avis publics peuvent être affichés.
  publicReviews(): Observable<Avis[]> {
    return this.http.get<Avis[]>('/api/avis', { timeout: 12000 });
  }
}
