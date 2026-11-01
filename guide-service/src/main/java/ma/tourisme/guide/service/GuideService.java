package ma.tourisme.guide.service;
import java.util.List;
import ma.tourisme.guide.dto.*;
import ma.tourisme.guide.model.Guide;
import ma.tourisme.guide.repository.GuideRepository;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class GuideService {
  private final GuideRepository repo;
  public GuideService(GuideRepository repo) { this.repo=repo; }
  public List<Guide> list(String ville,String langue,boolean admin) {return repo.list(ville,langue,admin);}
  public Guide get(long id) {return repo.find(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Guide introuvable"));}
  public Guide mine(String sub) {return repo.mine(sub).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Votre compte n’est pas encore associé à un profil Guide. Contactez un administrateur."));}
  public Guide save(Long id,GuideRequest r) {
    if(id!=null)get(id);
    try {
      var g=repo.save(id,r);
      if(g==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Guide introuvable");
      return g;
    } catch(DuplicateKeyException ex) {throw new ResponseStatusException(HttpStatus.CONFLICT,"Ce compte possède déjà un profil Guide.");}
  }
  public Guide updateMine(String sub,ProfileRequest r) {
    var g=repo.updateMine(sub,r);
    if(g==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Votre profil Guide n’est plus disponible.");
    return g;
  }
  public void archive(long id) {
    if(!repo.archive(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Guide introuvable");
  }
}
