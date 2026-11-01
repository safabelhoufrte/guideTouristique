package ma.tourisme.avis.controller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import ma.tourisme.avis.dto.*;
import ma.tourisme.avis.service.AvisService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/avis")
public class AvisController {
  private final AvisService service;
  public AvisController(AvisService service){this.service=service;}
  @GetMapping public List<AvisResponse> list(@RequestParam(required=false)Long circuitId,@RequestParam(required=false)Long guideId){return service.list(circuitId,guideId,false);}
  @GetMapping("/admin") public List<AvisResponse> all(){return service.list(null,null,true);}
  @GetMapping("/me") public List<AvisResponse> mine(@AuthenticationPrincipal Jwt jwt){return service.mine(jwt);}
  @PostMapping @ResponseStatus(HttpStatus.CREATED) public AvisResponse create(@Valid @RequestBody AvisRequest r,@AuthenticationPrincipal Jwt jwt){return service.create(r,jwt);}
  public record Publication(@NotNull Boolean publie){}
  @PatchMapping("/{id}/publication") public AvisResponse publication(@PathVariable long id,@Valid @RequestBody Publication r){return service.publication(id,r.publie());}
}
