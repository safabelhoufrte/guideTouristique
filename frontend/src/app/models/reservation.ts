export type StatutReservation = 'EN_ATTENTE' | 'ACCEPTEE' | 'REFUSEE' | 'ANNULEE';

// Ce que la page lit des autres services
export interface CircuitResume {
  id: number;
  titre: string;
  guideId: number;
  dureeHeures: number | null;
  prix: number;
}

export interface GuideResume {
  id: number;
  nom: string;
}

export interface ReservationRequest {
  circuitId: number;
  dateVisite: string;
  nbPersonnes: number;
  nom: string;
  email: string;
}

export interface Reservation extends ReservationRequest {
  id: number;
  guideId: number;
  prixTotal: number;
  statut: StatutReservation;
  dateCreation: string;
}
