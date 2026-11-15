
package ma.tourisme.guide.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record GuideLoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
) {}