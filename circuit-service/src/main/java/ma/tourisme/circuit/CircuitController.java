package ma.tourisme.circuit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@RestController
public class CircuitController {
    private final JdbcTemplate db;
    private final GuideClient guideClient;

    public CircuitController(JdbcTemplate db, GuideClient guideClient) {
        this.db = db;
        this.guideClient = guideClient;
    }

    public record Input(@NotBlank @Size(max = 160) String titre,
            @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal prix,
            @Positive long guideId, @NotNull Boolean disponible) {
    }

    private Circuit map(java.sql.ResultSet r, int row) throws java.sql.SQLException {
        return new Circuit(r.getLong("id"), r.getString("titre"), r.getBigDecimal("prix"), r.getLong("guide_id"),
                r.getBoolean("disponible"));
    }

    @GetMapping("/circuits")
    public List<Circuit> list(@RequestParam(required = false) Long guideId) {
        return guideId == null ? db.query("SELECT * FROM circuits ORDER BY id", this::map)
                : db.query("SELECT * FROM circuits WHERE guide_id=? ORDER BY id", this::map, guideId);
    }

    @GetMapping("/guides/{guideId}/circuits")
    public List<Circuit> byGuide(@PathVariable long guideId) {
        return list(guideId);
    }

    @GetMapping("/circuits/{id}")
    public Circuit get(@PathVariable long id) {
        return db.query("SELECT * FROM circuits WHERE id=?", this::map, id).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Circuit introuvable"));
    }

    @PostMapping("/circuits")
    @ResponseStatus(HttpStatus.CREATED)
    public Circuit create(@Valid @RequestBody Input input) {
        guideClient.requireActive(input.guideId());
        Long id = db.queryForObject(
                "SELECT id FROM FINAL TABLE (INSERT INTO circuits(titre,prix,guide_id,disponible) VALUES (?,?,?,?))",
                Long.class,
                input.titre(), input.prix(), input.guideId(), input.disponible());
        return get(id);
    }

    @PutMapping("/circuits/{id}")
    public Circuit update(@PathVariable long id, @Valid @RequestBody Input input) {
        get(id);
        guideClient.requireActive(input.guideId());
        db.update("UPDATE circuits SET titre=?,prix=?,guide_id=?,disponible=? WHERE id=?", input.titre(), input.prix(),
                input.guideId(), input.disponible(), id);
        return get(id);
    }

    @DeleteMapping("/circuits/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable long id) {
        get(id);
        db.update("UPDATE circuits SET disponible=FALSE WHERE id=?", id);
    }

    // Used by reservations: rechecks that the guide is still active before booking.
    @GetMapping("/circuits/{id}/reservation-details")
    public Circuit bookable(@PathVariable long id) {
        Circuit circuit = get(id);
        if (!circuit.disponible())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Circuit indisponible");
        guideClient.requireActive(circuit.guideId());
        return circuit;
    }
}
