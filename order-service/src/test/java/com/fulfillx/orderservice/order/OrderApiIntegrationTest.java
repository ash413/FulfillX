package com.fulfillx.orderservice.order;

import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.*; // delete this line if it's red; not needed
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @LocalServerPort
    int port;

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }

    @Test
    void createsAndFetchesAnOrder() {
        var request = new CreateOrderRequest(
                382L, List.of(new CreateOrderRequest.Item(102L, 2)));

        OrderResponse created = client().post().uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(OrderResponse.class);

        assertThat(created.id()).isNotNull();
        assertThat(created.status()).isEqualTo(OrderStatus.PENDING);

        OrderResponse fetched = client().get().uri("/orders/{id}", created.id())
                .retrieve()
                .body(OrderResponse.class);

        assertThat(fetched.customerId()).isEqualTo(382L);
        assertThat(fetched.items()).hasSize(1);
    }

    @Test
    void returns404ForUnknownOrder() {
        HttpStatus status = client().get().uri("/orders/999999")
                .exchange((req, res) -> HttpStatus.valueOf(res.getStatusCode().value()));

        assertThat(status).isEqualTo(HttpStatus.NOT_FOUND);
    }
}