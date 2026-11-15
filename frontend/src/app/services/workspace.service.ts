import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Guide } from '../models/circuit';
import { Reservation } from '../models/reservation';

export interface GuideProfile extends Guide {
  tarifParJour: number;
  ownerSubject: string | null;
  email: string | null;
  telephone: string | null;
  actif: boolean;
}
export interface Avis {
  id: number;
  reservationId: number;
  circuitId: number;
  guideId: number;
  auteur: string;
  noteGuide: number;
  noteCircuit: number;
  commentaire: string;
  dateCreation: string;
  publie: boolean;
}
export interface Eligibility {
  reservationId: number;
  circuitId: number;
  guideId: number;
  eligible: boolean;
  raison: string;
}
export function apiError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 401) return 'Votre session a expiré. Reconnectez-vous.';
    if (error.status === 403) return 'Vous n’avez pas les permissions nécessaires.';
    if (error.status === 0 || error.status >= 500)
      return 'Un service est momentanément indisponible. Réessayez.';
    return error.error?.message || 'La demande n’a pas pu aboutir. Vérifiez les informations.';
  }
  return error instanceof Error ? error.message : 'Une erreur est survenue.';
}

@Injectable({ providedIn: 'root' })
export class WorkspaceService {
  private readonly http = inject(HttpClient);
  reservations(scope: 'me' | 'guide' | 'admin') {
    return this.http.get<Reservation[]>(
      '/api/reservations' + (scope === 'admin' ? '' : '/' + scope),
      { timeout: 8000 },
    );
  }
  decision(id: number, action: 'annuler' | 'accepter' | 'refuser') {
    return this.http.patch<Reservation>(`/api/reservations/${id}/${action}`, {}, { timeout: 8000 });
  }
  eligibility(id: number) {
    return this.http.get<Eligibility>(`/api/reservations/${id}/eligibilite`, { timeout: 8000 });
  }
  profile() {
    return this.http.get<GuideProfile>('/api/guides/me', { timeout: 8000 });
  }
  updateProfile(body: unknown) {
    return this.http.put<GuideProfile>('/api/guides/me', body, { timeout: 8000 });
  }
  assignedCircuits() {
    return this.http.get<import('../models/circuit').Circuit[]>('/api/guides/me/circuits', {
      timeout: 8000,
    });
  }
  adminGuides() {
    return this.http.get<GuideProfile[]>('/api/guides/admin', { timeout: 8000 });
  }
  saveGuide(id: number | null, body: unknown) {
    return id === null
      ? this.http.post<GuideProfile>('/api/guides', body)
      : this.http.put<GuideProfile>(`/api/guides/${id}`, body);
  }
  archiveGuide(id: number) {
    return this.http.delete<void>(`/api/guides/${id}`);
  }
  avis(scope: 'public' | 'me' | 'admin') {
    return this.http.get<Avis[]>('/api/avis' + (scope === 'public' ? '' : '/' + scope), {
      timeout: 8000,
    });
  }
  createAvis(body: unknown) {
    return this.http.post<Avis>('/api/avis', body, { timeout: 10000 });
  }
  publish(id: number, publie: boolean) {
    return this.http.patch<Avis>(`/api/avis/${id}/publication`, { publie });
  }
}
