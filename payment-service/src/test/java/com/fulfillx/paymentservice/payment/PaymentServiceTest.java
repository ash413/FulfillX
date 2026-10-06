package com.fulfillx.paymentservice.payment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private final PaymentRepository repository = mock(PaymentRepository.class);
    private final PaymentService service = new PaymentService(repository);

    PaymentServiceTest() {
        when(repository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void completesPaymentForOrdinaryOrder() {
        Payment payment = service.process(7L).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getFailureReason()).isNull();
    }

    @Test
    void declinesEveryFifthOrder() {
        Payment payment = service.process(10L).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo("Card declined (simulated)");
    }

    @Test
    void ignoresDuplicateOrders() {
        when(repository.existsByOrderId(7L)).thenReturn(true);

        assertThat(service.process(7L)).isEmpty();
        verify(repository, never()).save(any());
    }
}