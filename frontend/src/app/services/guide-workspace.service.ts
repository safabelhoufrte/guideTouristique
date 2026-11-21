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
  photo?: string | null;
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

export type ReservationStatut =
  | 'EN_ATTENTE'
  | 'ACCEPTEE'
  | 'REFUSEE'
  | 'ANNULEE';

export interface GuideReservation {
  id: number;
  circuitId: number;
  guideId: number;
  dateVisite: string;
  nbPersonnes: number;
  nom: string;
  email: string;
  prixTotal: number;
  statut: ReservationStatut;
  dateCreation: string;
  codePromo: string | null;
  reductionPct: number;
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

  uploadMaPhoto(file: File): Observable<void> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.put<void>(
      '/api/guides/me/photo',
      formData,
      { timeout: 20000 }
    );
  }

  supprimerMaPhoto(): Observable<void> {
    return this.http.delete<void>(
      '/api/guides/me/photo',
      { timeout: 12000 }
    );
  }
  circuits(guideId: number): Observable<Circuit[]> {
    return this.http.get<Circuit[]>('/api/circuits', {
      params: new HttpParams().set('guideId', guideId), timeout: 12000
    });
  }

  reservationsDuGuide(): Observable<GuideReservation[]> {
    return this.http.get<GuideReservation[]>(
      '/api/reservations/guide',
      { timeout: 20000 }
    );
  }


  accepterReservation(id: number): Observable<GuideReservation> {
    return this.http.patch<GuideReservation>(
      `/api/reservations/guide/${id}/accepter`,
      {}
    );
  }

  refuserReservation(id: number): Observable<GuideReservation> {
    return this.http.patch<GuideReservation>(
      `/api/reservations/guide/${id}/refuser`,
      {}
    );
  }



  // Consultation uniquement : il faut une route sécurisée « mes réservations »
  // côté service Réservation avant de pouvoir afficher des demandes privées.
  // Même exigence pour les avis privés ; les avis publics peuvent être affichés.
  publicReviews(): Observable<Avis[]> {
    return this.http.get<Avis[]>('/api/avis', { timeout: 12000 });
  }
}
