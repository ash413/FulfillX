package com.fulfillx.orderservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.email}")
    private String email;

    @Value("${app.bootstrap-admin.password}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (users.existsByEmail(email)) {
            return;
        }
        AppUser admin = new AppUser();
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        users.save(admin);
        log.info("Created bootstrap admin user {}", email);
    }
}