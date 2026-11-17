package ma.tourisme.reservation.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "codes_promo")
public class CodePromo {

    @Id
    @Column(length = 40)
    private String code;

    @Column(nullable = false)
    private Integer pourcentage;

    @Column(nullable = false)
    private Boolean actif = true;

    @Column(name = "date_expiration")
    private LocalDate dateExpiration;

    @Column(name = "utilisations_max")
    private Integer utilisationsMax;

    @Column(nullable = false)
    private Integer utilisations = 0;

    @Column(name = "personnes_min", nullable = false)
    private Integer personnesMin = 1;

    @Column(name = "premiere_reservation_seulement", nullable = false)
    private Boolean premiereReservationSeulement = false;

    @Column(nullable = false)
    private Boolean bienvenue = false;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Integer getPourcentage() { return pourcentage; }
    public void setPourcentage(Integer pourcentage) { this.pourcentage = pourcentage; }
    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }
    public LocalDate getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDate dateExpiration) { this.dateExpiration = dateExpiration; }
    public Integer getUtilisationsMax() { return utilisationsMax; }
    public void setUtilisationsMax(Integer utilisationsMax) { this.utilisationsMax = utilisationsMax; }
    public Integer getUtilisations() { return utilisations; }
    public void setUtilisations(Integer utilisations) { this.utilisations = utilisations; }
    public Integer getPersonnesMin() { return personnesMin; }
    public void setPersonnesMin(Integer personnesMin) { this.personnesMin = personnesMin; }
    public Boolean getPremiereReservationSeulement() { return premiereReservationSeulement; }
    public void setPremiereReservationSeulement(Boolean v) { this.premiereReservationSeulement = v; }

    public Boolean getBienvenue() { return bienvenue; }
    public void setBienvenue(Boolean bienvenue) { this.bienvenue = bienvenue; }
}