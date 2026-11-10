package ma.tourisme.guide.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "guide")
public class Guide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String prenom;
    private String ville;
    private String email;
    private String telephone;
    private double tarifParJour;
    private boolean actif = true;
    private Double noteMoyenne;            // null = « non noté »

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "guide_langues", joinColumns = @JoinColumn(name = "guide_id"))
    @Column(name = "langue")
    private List<String> langues = new ArrayList<>();

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public double getTarifParJour() { return tarifParJour; }
    public void setTarifParJour(double tarifParJour) { this.tarifParJour = tarifParJour; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
    public Double getNoteMoyenne() { return noteMoyenne; }
    public void setNoteMoyenne(Double noteMoyenne) { this.noteMoyenne = noteMoyenne; }
    public List<String> getLangues() { return langues; }
    public void setLangues(List<String> langues) { this.langues = langues; }
}