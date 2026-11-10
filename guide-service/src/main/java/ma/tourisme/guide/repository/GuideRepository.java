package ma.tourisme.guide.repository;

import ma.tourisme.guide.model.Guide;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GuideRepository extends JpaRepository<Guide, Long> {
    List<Guide> findByActifTrue();
}