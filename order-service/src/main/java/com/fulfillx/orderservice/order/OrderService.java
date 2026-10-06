package com.fulfillx.orderservice.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository repository;
    private final OrderEventPublisher eventPublisher;   // new

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

        eventPublisher.publishOrderCreated(
                new OrderCreatedEvent(saved.getId(), saved.getCustomerId(), request.items()));

        return toResponse(repository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new OrderNotFoundException(id));
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
}