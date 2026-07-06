package org.mymarketapp.payment.controller;

import org.mymarketapp.payment.api.PaymentApi;
import org.mymarketapp.payment.model.BalanceResponse;
import org.mymarketapp.payment.model.PaymentRequest;
import org.mymarketapp.payment.model.PaymentResponse;
import org.mymarketapp.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class PaymentController implements PaymentApi {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public Mono<ResponseEntity<BalanceResponse>> getBalance(ServerWebExchange exchange) {
        return paymentService.getBalance()
                .map(balance -> ResponseEntity.ok(new BalanceResponse(balance)));
    }

    @Override
    public Mono<ResponseEntity<PaymentResponse>> pay(Mono<PaymentRequest> paymentRequest, ServerWebExchange exchange) {
        return paymentRequest
                .flatMap(request -> paymentService.pay(request.getAmount()))
                .map(result -> ResponseEntity.ok(new PaymentResponse(result.success(), result.balance())));
    }
}
