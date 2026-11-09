import { HttpErrorResponse } from '@angular/common/http';

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

export interface AvisMoyenne {
  cibleType: 'CIRCUIT' | 'GUIDE';
  cibleId: number;
  moyenne: number | null;
  nombreAvis: number;
}

export interface CreateAvisDto {
  reservationId: number;
  noteGuide: number;
  noteCircuit: number;
  commentaire: string;
}

export interface Eligibility {
  reservationId: number;
  circuitId: number;
  guideId: number;
  eligible: boolean;
  raison: string;
}

export function extractAvisError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 401) {
      return 'Votre session a expiré. Reconnectez-vous.';
    }
    if (error.status === 403) {
      return 'Vous n’avez pas les permissions nécessaires.';
    }
    if (error.status === 0) {
      return 'Le service est inaccessible. Vérifiez votre connexion réseau.';
    }
    if (error.status >= 500) {
      return 'Le service des avis est momentanément indisponible. Réessayez plus tard.';
    }
    if (error.status === 400 || error.status === 404 || error.status === 409) {
      return error.error?.message || 'La demande n’a pas pu aboutir. Vérifiez les informations.';
    }
    return error.error?.message || 'Une erreur est survenue lors du traitement de votre demande.';
  }
  return error instanceof Error ? error.message : 'Une erreur inattendue est survenue.';
}
