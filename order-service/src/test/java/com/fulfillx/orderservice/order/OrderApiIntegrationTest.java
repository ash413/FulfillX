package com.fulfillx.orderservice.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:3.8.0");

    @LocalServerPort
    int port;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }

    private String adminToken() {
        return TestAuth.adminToken(port);
    }

    private Long createOrder() {
        var request = new CreateOrderRequest(
                382L, List.of(new CreateOrderRequest.Item(102L, 2)));
        OrderResponse created = client().post().uri("/orders")
                .header("Authorization", "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(OrderResponse.class);
        return created.id();
    }

    private OrderStatus statusOf(Long id) {
        return orderRepository.findById(id).orElseThrow().getStatus();
    }

    private void send(String topic, Long orderId, String json) {
        kafkaTemplate.send(topic, String.valueOf(orderId), json);
    }

    private void awaitStatus(Long id, OrderStatus expected) {
        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(statusOf(id)).isEqualTo(expected));
    }

    @Test
    void createsAndFetchesAnOrder() {
        Long id = createOrder();

        OrderResponse fetched = client().get().uri("/orders/{id}", id)
                .header("Authorization", "Bearer " + adminToken())
                .retrieve()
                .body(OrderResponse.class);

        assertThat(fetched.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(fetched.customerId()).isEqualTo(382L);
        assertThat(fetched.items()).hasSize(1);
    }

    @Test
    void returns404ForUnknownOrder() {
        HttpStatus status = client().get().uri("/orders/999999")
                .header("Authorization", "Bearer " + adminToken())
                .exchange((req, res) -> HttpStatus.valueOf(res.getStatusCode().value()));

        assertThat(status).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void happyPathEndsConfirmed() {
        Long id = createOrder();

        send("inventory.reserved", id, "{\"orderId\":" + id + "}");
        awaitStatus(id, OrderStatus.STOCK_RESERVED);

        send("payment.completed", id, "{\"orderId\":" + id + "}");
        awaitStatus(id, OrderStatus.CONFIRMED);
    }

    @Test
    void paymentFailureCancelsOrder() {
        Long id = createOrder();

        send("inventory.reserved", id, "{\"orderId\":" + id + "}");
        awaitStatus(id, OrderStatus.STOCK_RESERVED);

        send("payment.failed", id, "{\"orderId\":" + id + ",\"reason\":\"declined\"}");
        awaitStatus(id, OrderStatus.CANCELLED);
    }

    @Test
    void stockFailureCancelsOrder() {
        Long id = createOrder();

        send("inventory.failed", id, "{\"orderId\":" + id + ",\"reason\":\"out of stock\"}");
        awaitStatus(id, OrderStatus.CANCELLED);
    }

    @Test
    void confirmsEvenIfPaymentEventArrivesFirst() {
        Long id = createOrder();

        send("payment.completed", id, "{\"orderId\":" + id + "}");
        awaitStatus(id, OrderStatus.CONFIRMED);
    }
}