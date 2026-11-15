import { Reservation, StatutReservation } from '../../models/reservation';
import { Avis } from '../../services/workspace.service';

export type AnalyticsPeriod = 30 | 90 | 365;

/** Calendar dates follow the business timezone; the period includes today. */
export function activityAnalytics(
  reservations: Reservation[],
  reviews: Avis[],
  days: AnalyticsPeriod,
  today: string,
) {
  const start = new Date(today + 'T00:00:00Z');
  start.setUTCDate(start.getUTCDate() - days + 1);
  const from = start.toISOString().slice(0, 10);
  const within = (date: string) => date.slice(0, 10) >= from && date.slice(0, 10) <= today;
  const bookings = reservations.filter((r) => within(r.dateCreation));
  const accepted = bookings.filter((r) => r.statut === 'ACCEPTEE');
  const published = reviews.filter((r) => r.publie && within(r.dateCreation));
  const statuses: { status: StatutReservation; label: string; count: number }[] = [
    { status: 'EN_ATTENTE', label: 'En attente', count: 0 },
    { status: 'ACCEPTEE', label: 'Acceptées', count: 0 },
    { status: 'REFUSEE', label: 'Refusées', count: 0 },
    { status: 'ANNULEE', label: 'Annulées', count: 0 },
  ];
  for (const item of statuses) item.count = bookings.filter((r) => r.statut === item.status).length;

  const months: { key: string; label: string; count: number }[] = [];
  const month = new Date(from.slice(0, 7) + '-01T00:00:00Z');
  const end = today.slice(0, 7);
  const monthFormat = new Intl.DateTimeFormat('fr-FR', {
    month: 'short',
    year: '2-digit',
    timeZone: 'UTC',
  });
  while (month.toISOString().slice(0, 7) <= end) {
    const key = month.toISOString().slice(0, 7);
    months.push({
      key,
      label: monthFormat.format(month),
      count: bookings.filter((r) => r.dateCreation.startsWith(key)).length,
    });
    month.setUTCMonth(month.getUTCMonth() + 1);
  }

  const ranked = new Map<number, { id: number; count: number; accepted: number }>();
  for (const booking of bookings) {
    const row = ranked.get(booking.circuitId) ?? { id: booking.circuitId, count: 0, accepted: 0 };
    row.count++;
    if (booking.statut === 'ACCEPTEE') row.accepted++;
    ranked.set(row.id, row);
  }
  const cancelled = bookings.filter((r) => r.statut === 'ANNULEE').length;
  return {
    from,
    until: today,
    bookings: bookings.length,
    participants: accepted.reduce((sum, r) => sum + r.nbPersonnes, 0),
    // Sum the historical booked amounts in cents, independently of current catalogue prices.
    acceptedAmount: accepted.reduce((sum, r) => sum + Math.round(r.prixTotal * 100), 0) / 100,
    cancellationRate: bookings.length ? cancelled / bookings.length : null,
    statuses,
    months,
    maxMonthlyCount: Math.max(1, ...months.map((m) => m.count)),
    topCircuits: [...ranked.values()].sort((a, b) => b.count - a.count || a.id - b.id).slice(0, 5),
    publishedReviews: published.length,
    guideRating: published.length
      ? published.reduce((sum, r) => sum + r.noteGuide, 0) / published.length
      : null,
    circuitRating: published.length
      ? published.reduce((sum, r) => sum + r.noteCircuit, 0) / published.length
      : null,
  };
}
