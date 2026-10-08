package com.fulfillx.orderservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AppUser register(String email, String password) {
        String normalized = email.trim().toLowerCase();
        if (users.existsByEmail(normalized)) {
            throw new EmailAlreadyUsedException();
        }

        AppUser user = new AppUser();
        user.setEmail(normalized);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(Role.CUSTOMER);
        return users.save(user);
    }

    @Transactional(readOnly = true)
    public String login(String email, String password) {
        AppUser user = users.findByEmail(email.trim().toLowerCase())
                .filter(u -> passwordEncoder.matches(password, u.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return jwtService.issue(user);
    }
}