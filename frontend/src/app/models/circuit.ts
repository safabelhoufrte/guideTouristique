/** Wire contract of Circuit API: exactly one guideId per circuit. */
export interface Circuit {
  id: number;
  titre: string;
  description: string | null;
  ville: string | null;
  dureeHeures: number | null;
  prix: number;
  guideId: number;
}

export interface CircuitFilters {
  ville?: string;
  guideId?: number;
}

export interface Guide {
  id: number;
  nom: string;
  ville?: string;
  langues?: string[];
  disponible: boolean;
}
