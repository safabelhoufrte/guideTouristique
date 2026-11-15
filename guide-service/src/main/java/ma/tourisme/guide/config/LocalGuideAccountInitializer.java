package ma.tourisme.guide.config;

import ma.tourisme.guide.model.Guide;
import ma.tourisme.guide.model.GuideAccount;
import ma.tourisme.guide.repository.GuideRepository;
import ma.tourisme.guide.repository.GuideAccountRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
@Profile("local")
public class LocalGuideAccountInitializer
        implements ApplicationRunner {

    private final GuideRepository guides;
    private final GuideAccountRepository accounts;
    private final PasswordEncoder encoder;

    @Value("${app.local-account.enabled:false}")
    private boolean enabled;

    @Value("${app.local-account.guide-id:0}")
    private long guideId;

    @Value("${app.local-account.email:}")
    private String email;

    @Value("${app.local-account.password:}")
    private String password;

    public LocalGuideAccountInitializer(
            GuideRepository guides,
            GuideAccountRepository accounts,
            PasswordEncoder encoder) {

        this.guides = guides;
        this.accounts = accounts;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        if (!enabled) {
            return;
        }

        if (guideId <= 0 || email.isBlank()
                || password.length() < 12) {
            throw new IllegalStateException(
                "Configuration du compte local invalide");
        }

        String normalizedEmail =
            email.trim().toLowerCase(Locale.ROOT);

        if (accounts.existsByGuideId(guideId)
                || accounts.existsByEmailIgnoreCase(normalizedEmail)) {
            System.out.println(
                "Compte local deja existant : aucune modification");
            return;
        }

        Guide guide = guides.findById(guideId)
            .filter(Guide::isActif)
            .orElseThrow(() -> new IllegalStateException(
                "Guide actif introuvable : " + guideId));

        GuideAccount account = new GuideAccount();
        account.setGuide(guide);
        account.setEmail(normalizedEmail);
        account.setPasswordHash(encoder.encode(password));

        accounts.save(account);

        System.out.println(
            "Compte local cree pour le guide " + guideId);
    }
}