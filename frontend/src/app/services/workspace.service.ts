import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
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
  avis(_scope: 'public') {
    return this.http.get<Avis[]>('/api/avis', { timeout: 8000 });
  }
}
