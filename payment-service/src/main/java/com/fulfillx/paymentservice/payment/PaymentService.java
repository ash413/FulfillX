package com.fulfillx.paymentservice.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository payments;
    private final OutboxWriter outbox;

    /** Returns empty if this order was already processed (duplicate message). */
    @Transactional
    public Optional<Payment> process(Long orderId) {
        if (payments.existsByOrderId(orderId)) {
            return Optional.empty();
        }

        Payment payment = new Payment();
        payment.setOrderId(orderId);

        // Simulation: every 5th order id is declined.
        if (orderId % 5 == 0) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Card declined (simulated)");
        } else {
            payment.setStatus(PaymentStatus.COMPLETED);
        }

        Payment saved = payments.save(payment);

        if (saved.getStatus() == PaymentStatus.COMPLETED) {
            outbox.write("Payment", orderId, "payment.completed",
                    new PaymentCompletedEvent(orderId));
        } else {
            outbox.write("Payment", orderId, "payment.failed",
                    new PaymentFailedEvent(orderId, saved.getFailureReason()));
        }
        return Optional.of(saved);
    }
}