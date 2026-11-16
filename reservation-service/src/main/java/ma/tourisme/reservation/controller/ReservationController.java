@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
    private final ReservationService service;
    public ReservationController(ReservationService service) { this.service = service; }

    @PostMapping
    public Reservation creer(@RequestBody Reservation r) { return service.creer(r); }

    @GetMapping
    public List<Reservation> lister() { return service.lister(); }

    @GetMapping("/{id}")
    public Reservation trouver(@PathVariable Long id) { return service.trouver(id); }
}