package ma.tourisme.guide.dto;

import java.util.List;

/**
 * Admin view of a guide (GET /api/guides/admin): includes inactive guides and the linked
 * Keycloak account. Never returned by public routes.
 */
public record GuideAdminResponse(
    Long id,
    String nom,
    String prenom,
    String ville,
    List<String> langues,
    String email,
    String telephone,
    double tarifParJour,
    boolean disponible,
    boolean actif,
    String ownerSubject,
    Double noteMoyenne
) {}
