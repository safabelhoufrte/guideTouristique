import { Reservation, StatutReservation } from '../../models/reservation';
import { tabOf } from './tourist-reservations';

const TODAY = '2026-11-15';

function reservation(statut: StatutReservation, dateVisite: string): Reservation {
  return {
    id: 1,
    circuitId: 1,
    guideId: 1,
    dateVisite,
    nbPersonnes: 2,
    nom: 'Test',
    email: 'test@local.test',
    prixTotal: 100,
    statut,
    dateCreation: '2026-11-01',
  };
}

describe('tabOf', () => {
  it('puts upcoming reservations in En attente or Confirmées', () => {
    expect(tabOf(reservation('EN_ATTENTE', '2026-12-01'), TODAY)).toBe('attente');
    expect(tabOf(reservation('ACCEPTEE', '2026-12-01'), TODAY)).toBe('confirmees');
    expect(tabOf(reservation('ACCEPTEE', TODAY), TODAY)).toBe('confirmees');
  });

  it('keeps cancelled and refused reservations in Annulées, past or future', () => {
    expect(tabOf(reservation('ANNULEE', '2026-12-01'), TODAY)).toBe('annulees');
    expect(tabOf(reservation('REFUSEE', '2026-10-01'), TODAY)).toBe('annulees');
  });

  it('moves past visits to Historique', () => {
    expect(tabOf(reservation('ACCEPTEE', '2026-11-14'), TODAY)).toBe('historique');
    expect(tabOf(reservation('EN_ATTENTE', '2026-10-01'), TODAY)).toBe('historique');
  });
});
