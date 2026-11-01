package ma.tourisme.guide.controller;
import java.util.List;
import jakarta.validation.Valid;
import ma.tourisme.guide.dto.*;
import ma.tourisme.guide.model.Guide;
import ma.tourisme.guide.service.GuideService;
import ma.tourisme.guide.client.CircuitClient;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/guides")
public class GuideController {
  private final GuideService service;
  private final CircuitClient circuits;
  public GuideController(GuideService service,CircuitClient circuits){this.service=service;this.circuits=circuits;}
  @GetMapping public List<GuideResponse> list(@RequestParam(required=false)String ville,@RequestParam(required=false)String langue){return service.list(ville,langue,false).stream().map(GuideResponse::from).toList();}
  @GetMapping("/{id}") public GuideResponse get(@PathVariable long id){return GuideResponse.from(service.get(id));}
  @GetMapping("/admin") public List<Guide> admin(){return service.list(null,null,true);}
  @GetMapping("/me") public Guide mine(@AuthenticationPrincipal Jwt jwt){return service.mine(jwt.getSubject());}
  @GetMapping("/me/circuits") public com.fasterxml.jackson.databind.JsonNode myCircuits(@AuthenticationPrincipal Jwt jwt){return circuits.assigned(service.mine(jwt.getSubject()).id());}
  @PutMapping("/me") public Guide updateMine(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody ProfileRequest r){return service.updateMine(jwt.getSubject(),r);}
  @PostMapping @ResponseStatus(HttpStatus.CREATED) public Guide create(@Valid @RequestBody GuideRequest r){return service.save(null,r);}
  @PutMapping("/{id}") public Guide update(@PathVariable long id,@Valid @RequestBody GuideRequest r){return service.save(id,r);}
  @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void archive(@PathVariable long id){service.archive(id);}
}
