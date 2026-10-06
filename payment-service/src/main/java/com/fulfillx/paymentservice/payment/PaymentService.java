package com.fulfillx.paymentservice.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository payments;

    /** Returns empty if this order was already processed (duplicate message). */
    @Transactional
    public Optional<Payment> process(Long orderId) {
        if (payments.existsByOrderId(orderId)) {
            return Optional.empty();
        }

        Payment payment = new Payment();
        payment.setOrderId(orderId);

        // Simulation: every 5th order id is declined, so failures are easy to reproduce.
        if (orderId % 5 == 0) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Card declined (simulated)");
        } else {
            payment.setStatus(PaymentStatus.COMPLETED);
        }

        return Optional.of(payments.save(payment));
    }
}