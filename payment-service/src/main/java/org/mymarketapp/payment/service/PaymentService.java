package org.mymarketapp.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class PaymentService {

    private final AtomicLong balance;

    public PaymentService(@Value("${payment.initial-balance}") long initialBalance) {
        this.balance = new AtomicLong(initialBalance);
    }

    public Mono<Long> getBalance() {
        return Mono.fromSupplier(balance::get);
    }

    public Mono<PaymentResult> pay(long amount) {
        return Mono.fromSupplier(() -> {
            while (true) {
                long current = balance.get();
                if (current < amount) {
                    return new PaymentResult(false, current);
                }
                long updated = current - amount;
                if (balance.compareAndSet(current, updated)) {
                    return new PaymentResult(true, updated);
                }
            }
        });
    }
}
