
package ma.tourisme.guide.dto;

public record GuideTokenResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds
) {}