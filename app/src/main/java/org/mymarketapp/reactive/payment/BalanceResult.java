package org.mymarketapp.reactive.payment;

public record BalanceResult(boolean available, long balance) {

    public static BalanceResult unavailable() {
        return new BalanceResult(false, 0L);
    }
}
