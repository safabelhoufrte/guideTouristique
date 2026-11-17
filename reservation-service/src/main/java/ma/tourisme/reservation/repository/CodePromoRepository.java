package ma.tourisme.reservation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.tourisme.reservation.model.CodePromo;

public interface CodePromoRepository extends JpaRepository<CodePromo, String> {

    List<CodePromo> findByBienvenueTrueAndActifTrue();
}