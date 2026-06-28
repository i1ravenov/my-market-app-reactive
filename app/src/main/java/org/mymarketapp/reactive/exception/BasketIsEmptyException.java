package org.mymarketapp.reactive.exception;

public class BasketIsEmptyException extends RuntimeException {

    public BasketIsEmptyException(String message) {
        super(message);
    }
}
