package ma.tourisme.avis.dto;

public record EligibleReservationDto(
    long reservationId,
    long circuitId,
    long guideId
) {}
