package org.mymarketapp.payment.service;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentServiceTest {

    private final PaymentService paymentService = new PaymentService(1000L);

    @Test
    void getBalanceReturnsInitialBalance() {
        StepVerifier.create(paymentService.getBalance())
                .expectNext(1000L)
                .verifyComplete();
    }

    @Test
    void payDeductsAmountWhenBalanceIsSufficient() {
        StepVerifier.create(paymentService.pay(400L))
                .assertNext(result -> {
                    assertThat(result.success()).isTrue();
                    assertThat(result.balance()).isEqualTo(600L);
                })
                .verifyComplete();

        StepVerifier.create(paymentService.getBalance())
                .expectNext(600L)
                .verifyComplete();
    }

    @Test
    void payFailsAndLeavesBalanceUnchangedWhenBalanceIsInsufficient() {
        StepVerifier.create(paymentService.pay(2000L))
                .assertNext(result -> {
                    assertThat(result.success()).isFalse();
                    assertThat(result.balance()).isEqualTo(1000L);
                })
                .verifyComplete();

        StepVerifier.create(paymentService.getBalance())
                .expectNext(1000L)
                .verifyComplete();
    }
}
