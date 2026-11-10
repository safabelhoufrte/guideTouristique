package ma.tourisme.guide.controller;

import jakarta.validation.Valid;
import ma.tourisme.guide.dto.GuideRequest;
import ma.tourisme.guide.dto.GuideResponse;
import ma.tourisme.guide.service.GuideService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/guides")
public class GuideController {

    private final GuideService service;

    public GuideController(GuideService service) {
        this.service = service;
    }

    @GetMapping
    public List<GuideResponse> getAll(@RequestParam(required = false) String ville,
                                      @RequestParam(required = false) String langue) {
        return service.findAll(ville, langue);
    }

    @GetMapping("/{id}")
    public GuideResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GuideResponse create(@Valid @RequestBody GuideRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public GuideResponse update(@PathVariable Long id, @Valid @RequestBody GuideRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.desactiver(id);
    }
}