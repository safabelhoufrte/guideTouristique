package ma.tourisme.guide.model;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
public record Guide(long id, String nom, String ville, List<String> langues,
  BigDecimal tarifParJour, boolean disponible, UUID ownerSubject, String email,
  String telephone, boolean actif) {}
