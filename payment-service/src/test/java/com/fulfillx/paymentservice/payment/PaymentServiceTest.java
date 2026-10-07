package com.fulfillx.paymentservice.payment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private final PaymentRepository repository = mock(PaymentRepository.class);
    private final OutboxWriter outbox = mock(OutboxWriter.class);
    private final PaymentService service = new PaymentService(repository, outbox);

    PaymentServiceTest() {
        when(repository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void completesPaymentAndWritesCompletedEvent() {
        Payment payment = service.process(7L).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getFailureReason()).isNull();
        verify(outbox).write(eq("Payment"), eq(7L), eq("payment.completed"), any());
    }

    @Test
    void declinesEveryFifthOrderAndWritesFailedEvent() {
        Payment payment = service.process(10L).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo("Card declined (simulated)");
        verify(outbox).write(eq("Payment"), eq(10L), eq("payment.failed"), any());
    }

    @Test
    void ignoresDuplicateOrdersAndWritesNothing() {
        when(repository.existsByOrderId(7L)).thenReturn(true);

        assertThat(service.process(7L)).isEmpty();
        verify(repository, never()).save(any());
        verify(outbox, never()).write(any(), any(), any(), any());
    }
}