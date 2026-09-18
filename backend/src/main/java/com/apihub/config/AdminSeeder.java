package com.apihub.config;

import com.apihub.entity.Role;
import com.apihub.entity.User;
import com.apihub.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first ADMIN account so the admin screens are reachable.
 *
 * It runs ONLY when both ADMIN_EMAIL and ADMIN_PASSWORD are set in .env —
 * there is deliberately no default admin password baked into the source,
 * because a hardcoded credential in a public repo is exactly the mistake this
 * project is cleaning up from the old Node backend.
 */
@Component
@Order(2)
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       @Value("${ADMIN_EMAIL:}") String adminEmail,
                       @Value("${ADMIN_PASSWORD:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.info("ADMIN_EMAIL/ADMIN_PASSWORD not set — skipping admin seeding");
            return;
        }
        if (userRepository.existsByEmailIgnoreCase(adminEmail)) {
            return;
        }
        userRepository.save(new User("Administrator", adminEmail.toLowerCase(),
                passwordEncoder.encode(adminPassword), Role.ADMIN));
        log.info("Created ADMIN account for {}", adminEmail);
    }
}
