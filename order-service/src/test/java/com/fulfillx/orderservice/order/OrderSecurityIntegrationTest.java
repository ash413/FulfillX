package com.fulfillx.orderservice.order;

import com.fulfillx.orderservice.security.AuthController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderSecurityIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:3.8.0");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @LocalServerPort
    int port;

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }

    // ---------- helpers ----------

    private static CreateOrderRequest request(long customerId) {
        return new CreateOrderRequest(customerId, List.of(new CreateOrderRequest.Item(102L, 1)));
    }

    private int getStatus(String uri, String token) {
        return client().get().uri(uri)
                .headers(h -> { if (token != null) h.setBearerAuth(token); })
                .exchange((req, res) -> res.getStatusCode().value());
    }

    private int postOrderStatus(String token, long customerId) {
        return client().post().uri("/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request(customerId))
                .exchange((req, res) -> res.getStatusCode().value());
    }

    private OrderResponse createOrder(String token, long customerId, String idempotencyKey) {
        RestClient.RequestBodySpec spec = client().post().uri("/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON);
        if (idempotencyKey != null) {
            spec = spec.header("Idempotency-Key", idempotencyKey);
        }
        return spec.body(request(customerId)).retrieve().body(OrderResponse.class);
    }

    private int registerStatus(String email, String password) {
        return client().post().uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AuthController.RegisterRequest(email, password))
                .exchange((req, res) -> res.getStatusCode().value());
    }

    private int loginStatus(String email, String password) {
        return client().post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AuthController.LoginRequest(email, password))
                .exchange((req, res) -> res.getStatusCode().value());
    }

    // ---------- authentication ----------

    @Test
    void requestWithoutTokenIsRejected() {
        assertThat(getStatus("/orders/1", null)).isEqualTo(401);
    }

    @Test
    void garbageTokenIsRejected() {
        assertThat(getStatus("/orders/1", "not-a-real-token")).isEqualTo(401);
    }

    @Test
    void tokenWithTamperedRolesIsRejected() {
        String token = TestAuth.newCustomer(port).token();
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String forgedPayload = payload.replace("CUSTOMER", "ADMIN");
        String forgedToken = parts[0] + "."
                + Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(forgedPayload.getBytes(StandardCharsets.UTF_8))
                + "." + parts[2];

        assertThat(forgedPayload).isNotEqualTo(payload);
        assertThat(getStatus("/admin/orders", forgedToken)).isEqualTo(401);
    }

    @Test
    void wrongPasswordIsRejected() {
        assertThat(loginStatus("admin@fulfillx.local", "definitely-wrong")).isEqualTo(401);
    }

    @Test
    void registeringTheSameEmailTwiceIsRejected() {
        String email = "dup-" + UUID.randomUUID() + "@example.com";

        assertThat(registerStatus(email, "password123")).isEqualTo(201);
        assertThat(registerStatus(email, "password123")).isEqualTo(409);
    }

    @Test
    void tooShortPasswordIsRejected() {
        assertThat(registerStatus("short-" + UUID.randomUUID() + "@example.com", "short")).isEqualTo(400);
    }

    // ---------- authorization and ownership ----------

    @Test
    void customerCanOrderForSelfAndReadItBack() {
        TestAuth.TestUser alice = TestAuth.newCustomer(port);

        OrderResponse created = createOrder(alice.token(), alice.id(), null);

        assertThat(created.customerId()).isEqualTo(alice.id());
        assertThat(getStatus("/orders/" + created.id(), alice.token())).isEqualTo(200);
    }

    @Test
    void customerCannotOrderForSomeoneElse() {
        TestAuth.TestUser alice = TestAuth.newCustomer(port);

        assertThat(postOrderStatus(alice.token(), alice.id() + 1000)).isEqualTo(403);
    }

    @Test
    void customerCannotReadAnotherCustomersOrder() {
        TestAuth.TestUser alice = TestAuth.newCustomer(port);
        TestAuth.TestUser bob = TestAuth.newCustomer(port);
        OrderResponse alicesOrder = createOrder(alice.token(), alice.id(), null);

        assertThat(getStatus("/orders/" + alicesOrder.id(), bob.token())).isEqualTo(404);
    }

    @Test
    void adminCanReadAnyCustomersOrder() {
        TestAuth.TestUser alice = TestAuth.newCustomer(port);
        OrderResponse alicesOrder = createOrder(alice.token(), alice.id(), null);

        assertThat(getStatus("/orders/" + alicesOrder.id(), TestAuth.adminToken(port))).isEqualTo(200);
    }

    @Test
    void customerCannotUseAdminEndpoint() {
        TestAuth.TestUser alice = TestAuth.newCustomer(port);

        assertThat(getStatus("/admin/orders", alice.token())).isEqualTo(403);
    }

    @Test
    void adminCanListOrders() {
        TestAuth.TestUser alice = TestAuth.newCustomer(port);
        createOrder(alice.token(), alice.id(), null);

        OrderResponse[] orders = client().get().uri("/admin/orders?size=5")
                .headers(h -> h.setBearerAuth(TestAuth.adminToken(port)))
                .retrieve()
                .body(OrderResponse[].class);

        assertThat(orders).isNotEmpty();
    }

    // ---------- idempotency keys are per user ----------

    @Test
    void sameIdempotencyKeyFromDifferentUsersCreatesTwoOrders() {
        TestAuth.TestUser alice = TestAuth.newCustomer(port);
        TestAuth.TestUser bob = TestAuth.newCustomer(port);
        String sharedKey = UUID.randomUUID().toString();

        OrderResponse alicesOrder = createOrder(alice.token(), alice.id(), sharedKey);
        OrderResponse bobsOrder = createOrder(bob.token(), bob.id(), sharedKey);

        assertThat(bobsOrder.id()).isNotEqualTo(alicesOrder.id());
        assertThat(bobsOrder.customerId()).isEqualTo(bob.id());
    }
}