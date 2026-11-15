
package ma.tourisme.guide.controller;

import jakarta.validation.Valid;
import ma.tourisme.guide.dto.GuideLoginRequest;
import ma.tourisme.guide.dto.GuideTokenResponse;
import ma.tourisme.guide.service.GuideAuthService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/guide-auth")
public class GuideAuthController {

    private final GuideAuthService authService;

    public GuideAuthController(GuideAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public GuideTokenResponse login(
            @Valid @RequestBody GuideLoginRequest request) {

        return authService.login(request);
    }
}