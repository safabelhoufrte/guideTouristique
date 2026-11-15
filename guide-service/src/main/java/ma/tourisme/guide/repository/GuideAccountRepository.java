
package ma.tourisme.guide.repository;

import ma.tourisme.guide.model.GuideAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuideAccountRepository
        extends JpaRepository<GuideAccount, Long> {

    Optional<GuideAccount> findByEmailIgnoreCase(String email);

    boolean existsByGuideId(Long guideId);

    boolean existsByEmailIgnoreCase(String email);
}