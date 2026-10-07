package com.fulfillx.orderservice.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderIdempotencyIntegrationTest {

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

    @Autowired
    OrderRepository orderRepository;

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }

    private static CreateOrderRequest request(long customerId) {
        return new CreateOrderRequest(customerId, List.of(new CreateOrderRequest.Item(102L, 1)));
    }

    private ResponseEntity<OrderResponse> post(CreateOrderRequest request, String key) {
        return client().post().uri("/orders")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(OrderResponse.class);
    }

    @Test
    void repeatedKeyReturnsOriginalOrderAndCreatesNoSecondOrder() {
        String key = UUID.randomUUID().toString();
        long before = orderRepository.count();

        ResponseEntity<OrderResponse> first = post(request(901L), key);
        ResponseEntity<OrderResponse> second = post(request(901L), key);

        assertThat(first.getStatusCode().value()).isEqualTo(201);
        assertThat(second.getStatusCode().value()).isEqualTo(200);
        assertThat(second.getBody().id()).isEqualTo(first.getBody().id());
        assertThat(second.getHeaders().getFirst("Idempotent-Replayed")).isEqualTo("true");
        assertThat(orderRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void differentKeysCreateDifferentOrders() {
        ResponseEntity<OrderResponse> first = post(request(902L), UUID.randomUUID().toString());
        ResponseEntity<OrderResponse> second = post(request(902L), UUID.randomUUID().toString());

        assertThat(second.getBody().id()).isNotEqualTo(first.getBody().id());
    }

    @Test
    void sameKeyWithDifferentBodyIsRejected() {
        String key = UUID.randomUUID().toString();
        post(request(903L), key);

        int status = client().post().uri("/orders")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request(904L))
                .exchange((req, res) -> res.getStatusCode().value());

        assertThat(status).isEqualTo(422);
    }
}