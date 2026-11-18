package com.refind.security;

import com.refind.entity.Role;
import com.refind.entity.User;
import com.refind.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrapService implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminBootstrapService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.bootstrap.enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${app.admin.bootstrap.email:admin@psgitech.ac.in}")
    private String adminEmail;

    @Value("${app.admin.bootstrap.password:Admin@12345}")
    private String adminPassword;

    @Value("${app.admin.bootstrap.name:System Administrator}")
    private String adminName;

    public AdminBootstrapService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!bootstrapEnabled) {
            logger.info("Admin bootstrap is disabled");
            return;
        }

        String normalizedEmail = adminEmail.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            logger.info("Admin account already exists for email: {}", normalizedEmail);
            return;
        }

        User adminUser = new User();
        adminUser.setName(adminName.trim());
        adminUser.setEmail(normalizedEmail);
        adminUser.setPasswordHash(passwordEncoder.encode(adminPassword));
        adminUser.setRole(Role.ROLE_ADMIN);

        userRepository.save(adminUser);
        logger.info("Idempotent admin user successfully bootstrapped with email: {}", normalizedEmail);
    }
}
