package com.fulfillx.orderservice.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository repository;
    private final OutboxWriter outbox;

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        Order order = new Order();
        order.setCustomerId(request.customerId());
        order.setStatus(OrderStatus.PENDING);

        for (CreateOrderRequest.Item i : request.items()) {
            OrderItem item = new OrderItem();
            item.setProductId(i.productId());
            item.setQuantity(i.quantity());
            order.addItem(item);
        }

        Order saved = repository.save(order);

        outbox.write("Order", saved.getId(), "order.created",
                new OrderCreatedEvent(saved.getId(), saved.getCustomerId(), request.items()));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list(int page, int size) {
        return repository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")))
                .map(this::toResponse)
                .getContent();
    }

    @Transactional
    public void markStockReserved(Long orderId) {
        repository.findById(orderId).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.STOCK_RESERVED);
            }
        });
    }

    @Transactional
    public void cancel(Long orderId) {
        repository.findById(orderId).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.PENDING
                    || order.getStatus() == OrderStatus.STOCK_RESERVED) {
                order.setStatus(OrderStatus.CANCELLED);
            }
        });
    }

    @Transactional
    public void confirm(Long orderId) {
        repository.findById(orderId).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.PENDING
                    || order.getStatus() == OrderStatus.STOCK_RESERVED) {
                order.setStatus(OrderStatus.CONFIRMED);
            }
        });
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getItems().stream()
                        .map(i -> new CreateOrderRequest.Item(i.getProductId(), i.getQuantity()))
                        .toList());
    }
}