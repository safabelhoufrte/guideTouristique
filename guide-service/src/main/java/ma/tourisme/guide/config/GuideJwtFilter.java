package ma.tourisme.guide.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import ma.tourisme.guide.service.GuideAuthService;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class GuideJwtFilter extends OncePerRequestFilter {

    private final GuideAuthService authService;

    public GuideJwtFilter(GuideAuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7);
                Long guideId = authService.verifyToken(token);

                var authentication =
                    new UsernamePasswordAuthenticationToken(
                        "guide:" + guideId,
                        null,
                        List.of(new SimpleGrantedAuthority(
                            "ROLE_GUIDE"))
                    );

                SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            } catch (RuntimeException e) {
                SecurityContextHolder.clearContext();
                response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Token invalide ou expire"
                );
                return;
            }
        }

        chain.doFilter(request, response);
    }
}