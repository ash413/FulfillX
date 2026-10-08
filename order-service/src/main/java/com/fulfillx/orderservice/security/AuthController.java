package com.fulfillx.orderservice.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    public record RegisterRequest(@NotBlank @Email String email,
                                  @NotBlank @Size(min = 8, max = 72) String password) { }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) { }

    public record RegisterResponse(Long id, String email) { }

    public record TokenResponse(String token, String tokenType, long expiresInSeconds) { }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        AppUser user = authService.register(request.email(), request.password());
        return new RegisterResponse(user.getId(), user.getEmail());
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.email(), request.password());
        return new TokenResponse(token, "Bearer", JwtService.TTL.toSeconds());
    }
}