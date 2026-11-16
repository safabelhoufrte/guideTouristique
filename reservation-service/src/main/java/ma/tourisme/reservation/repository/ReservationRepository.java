public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByEmail(String email);
    List<Reservation> findByStatut(String statut);
    List<Reservation> findByCircuitIdAndDateVisite(Long circuitId, LocalDate dateVisite);
}