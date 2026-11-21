package ma.tourisme.avis.dto;

import java.util.List;

public record UserEligibilityResponse(
    boolean eligible,
    List<EligibleReservationDto> reservations
) {}
