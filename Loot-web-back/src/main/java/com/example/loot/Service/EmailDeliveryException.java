package com.example.loot.Service;

/** Transport-neutral failure containing no provider secrets or response body. */
public class EmailDeliveryException extends RuntimeException {
    public EmailDeliveryException(String message) {
        super(message);
    }
}
