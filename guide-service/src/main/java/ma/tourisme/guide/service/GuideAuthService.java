package ma.tourisme.guide.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;

import ma.tourisme.guide.dto.GuideLoginRequest;
import ma.tourisme.guide.dto.GuideTokenResponse;
import ma.tourisme.guide.model.Guide;
import ma.tourisme.guide.model.GuideAccount;
import ma.tourisme.guide.repository.GuideAccountRepository;
import ma.tourisme.guide.repository.GuideRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;

@Service
public class GuideAuthService {

    private final GuideAccountRepository accounts;
    private final GuideRepository guides;
    private final PasswordEncoder encoder;
    private final SecretKey key;

    private static final long TTL_SECONDS = 3600;

    public GuideAuthService(
            GuideAccountRepository accounts,
            GuideRepository guides,
            PasswordEncoder encoder,
            @Value("${app.jwt.secret-base64}") String secret) {

        this.accounts = accounts;
        this.guides = guides;
        this.encoder = encoder;

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                "Cle JWT Base64 invalide", e);
        }

        if (bytes.length < 32) {
            throw new IllegalStateException(
                "La cle JWT doit contenir au moins 32 octets");
        }

        this.key = Keys.hmacShaKeyFor(bytes);
    }

    @Transactional(readOnly = true)
    public GuideTokenResponse login(GuideLoginRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        GuideAccount account = accounts.findByEmailIgnoreCase(email)
                .orElseThrow(this::invalidCredentials);

        if (!encoder.matches(
                request.password(), account.getPasswordHash())) {
            throw invalidCredentials();
        }

        if (!account.getGuide().isActif()) {
            throw invalidCredentials();
        }

        Instant now = Instant.now();

        String token = Jwts.builder()
                .subject(account.getGuide().getId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(
                    now.plusSeconds(TTL_SECONDS)))
                .signWith(key)
                .compact();

        return new GuideTokenResponse(
                token, "Bearer", TTL_SECONDS);
    }

    @Transactional(readOnly = true)
    public Long verifyToken(String token) {
        try {
            String subject = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();

            Long guideId = Long.parseLong(subject);

            boolean valid = accounts.existsByGuideId(guideId)
                && guides.findById(guideId)
                    .filter(Guide::isActif)
                    .isPresent();

            if (!valid) {
                throw invalidCredentials();
            }

            return guideId;

        } catch (JwtException | IllegalArgumentException e) {
            throw invalidCredentials();
        }
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "Email ou mot de passe invalide"
        );
    }
}