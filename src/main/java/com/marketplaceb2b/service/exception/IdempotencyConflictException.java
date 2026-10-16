package com.marketplaceb2b.service.exception;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String idempotencyKey) {
        super("Idempotency key was already used for a different quote: " + idempotencyKey);
    }
}
