package ma.tourisme.avis.dto;

public record MoyenneResponse(
    String cibleType,
    Long cibleId,
    Double moyenne,
    long nombreAvis
) {}
