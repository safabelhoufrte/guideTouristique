
package ma.tourisme.guide.repository;

import ma.tourisme.guide.model.GuidePhoto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuidePhotoRepository
        extends JpaRepository<GuidePhoto, Long> {
}