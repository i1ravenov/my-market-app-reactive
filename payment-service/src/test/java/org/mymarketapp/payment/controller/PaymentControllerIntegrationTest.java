package org.mymarketapp.payment.controller;

import org.junit.jupiter.api.Test;
import org.mymarketapp.payment.model.BalanceResponse;
import org.mymarketapp.payment.model.PaymentRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Balance is a shared, mutable, singleton value for the whole Spring context (reused across
 * test methods), so assertions here are expressed relative to the balance observed right
 * before each call rather than fixed absolute numbers - this keeps the tests independent of
 * execution order and of each other.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
class PaymentControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    private long currentBalance() {
        BalanceResponse response = webTestClient.get().uri("/balance")
                .exchange()
                .expectStatus().isOk()
                .expectBody(BalanceResponse.class)
                .returnResult()
                .getResponseBody();
        return response.getBalance();
    }

    @Test
    void getBalanceReturnsANumber() {
        webTestClient.get().uri("/balance")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isNumber();
    }

    @Test
    void payWithSufficientBalanceDeductsExactAmountAndReportsSuccess() {
        long balanceBefore = currentBalance();
        long amount = 1;

        webTestClient.post().uri("/pay")
                .bodyValue(new PaymentRequest(amount))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.balance").isEqualTo(balanceBefore - amount);
    }

    @Test
    void payWithExcessiveAmountReturnsSuccessFalseAndLeavesBalanceUnchanged() {
        long balanceBefore = currentBalance();

        webTestClient.post().uri("/pay")
                .bodyValue(new PaymentRequest(balanceBefore + 1))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.balance").isEqualTo(balanceBefore);
    }

    @Test
    void payWithInvalidAmountIsRejected() {
        webTestClient.post().uri("/pay")
                .bodyValue(new PaymentRequest(0L))
                .exchange()
                .expectStatus().is4xxClientError();
    }
}
