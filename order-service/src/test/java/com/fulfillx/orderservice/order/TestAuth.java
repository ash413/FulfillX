package com.fulfillx.orderservice.order;

import com.fulfillx.orderservice.security.AuthController;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.UUID;

final class TestAuth {

    record TestUser(Long id, String token) { }

    private TestAuth() { }

    private static RestClient client(int port) {
        return RestClient.create("http://localhost:" + port);
    }

    static String login(int port, String email, String password) {
        AuthController.TokenResponse response = client(port).post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AuthController.LoginRequest(email, password))
                .retrieve()
                .body(AuthController.TokenResponse.class);
        return response.token();
    }

    /** The admin account is created at startup from application.yaml. */
    static String adminToken(int port) {
        return login(port, "admin@fulfillx.local", "Admin12345!");
    }

    /** Registers a brand-new customer with a unique email and logs them in. */
    static TestUser newCustomer(int port) {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        AuthController.RegisterResponse registered = client(port).post().uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AuthController.RegisterRequest(email, "password123"))
                .retrieve()
                .body(AuthController.RegisterResponse.class);
        return new TestUser(registered.id(), login(port, email, "password123"));
    }
}