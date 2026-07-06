package org.mymarketapp.reactive.payment;

import org.mymarketapp.reactive.paymentclient.api.PaymentApi;
import org.mymarketapp.reactive.paymentclient.model.PaymentRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class PaymentClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final PaymentApi paymentApi;

    public PaymentClient(PaymentApi paymentApi) {
        this.paymentApi = paymentApi;
    }

    public Mono<BalanceResult> getBalance() {
        return paymentApi.getBalance()
                .timeout(TIMEOUT)
                .map(response -> new BalanceResult(true, response.getBalance()))
                .onErrorReturn(BalanceResult.unavailable());
    }

    public Mono<PaymentResult> pay(long amount) {
        return paymentApi.pay(new PaymentRequest().amount(amount))
                .timeout(TIMEOUT)
                .map(response -> new PaymentResult(true, response.getSuccess(), response.getBalance()))
                .onErrorReturn(PaymentResult.unavailable());
    }
}
