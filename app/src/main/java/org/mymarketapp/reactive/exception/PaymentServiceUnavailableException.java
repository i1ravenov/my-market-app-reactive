package org.mymarketapp.reactive.exception;

public class PaymentServiceUnavailableException extends RuntimeException {

    public PaymentServiceUnavailableException(String message) {
        super(message);
    }
}
