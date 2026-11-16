@Service
public class ReservationService {
    private final ReservationRepository repository;
    public ReservationService(ReservationRepository repository) { this.repository = repository; }

    @Transactional
    public Reservation creer(Reservation r) { return repository.save(r); }

    public List<Reservation> lister() { return repository.findAll(); }

    public Reservation trouver(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réservation introuvable"));
    }
}