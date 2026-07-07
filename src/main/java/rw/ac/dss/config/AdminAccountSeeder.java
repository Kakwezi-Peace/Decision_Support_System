package rw.ac.dss.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import rw.ac.dss.model.User;
import rw.ac.dss.repository.UserRepository;

/**
 * Seeds a single default admin account on first startup so there's always a way
 * to log in and create further accounts via POST /api/auth/register.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.default-admin-username}")
    private String defaultAdminUsername;

    @Value("${app.security.default-admin-password}")
    private String defaultAdminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(User.Role.ADMIN)) {
            return;
        }

        User admin = User.builder()
                .username(defaultAdminUsername)
                .password(passwordEncoder.encode(defaultAdminPassword))
                .fullName("Default Administrator")
                .role(User.Role.ADMIN)
                .enabled(true)
                .build();
        userRepository.save(admin);
        log.warn("Seeded default admin account '{}' with the password from application.properties. " +
                "Change it immediately via a real account before this goes anywhere near production.", defaultAdminUsername);
    }
}
