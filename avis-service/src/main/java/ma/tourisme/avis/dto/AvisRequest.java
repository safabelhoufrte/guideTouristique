package ma.tourisme.avis.dto;

import jakarta.validation.constraints.*;

public record AvisRequest(
    @NotNull(message = "L'identifiant de réservation est obligatoire")
    @Positive(message = "L'identifiant de réservation doit être positif")
    Long reservationId,

    @NotNull(message = "La note du guide est obligatoire")
    @Min(value = 1, message = "La note du guide doit être entre 1 et 5")
    @Max(value = 5, message = "La note du guide doit être entre 1 et 5")
    Integer noteGuide,

    @NotNull(message = "La note du circuit est obligatoire")
    @Min(value = 1, message = "La note du circuit doit être entre 1 et 5")
    @Max(value = 5, message = "La note du circuit doit être entre 1 et 5")
    Integer noteCircuit,

    @NotBlank(message = "Le commentaire ne peut pas être vide")
    @Size(max = 2000, message = "Le commentaire ne peut pas dépasser 2000 caractères")
    String commentaire
) {}
