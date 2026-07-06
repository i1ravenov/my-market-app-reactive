package org.mymarketapp.reactive.payment;

public record PaymentResult(boolean available, boolean success, long balance) {

    public static PaymentResult unavailable() {
        return new PaymentResult(false, false, 0L);
    }
}
