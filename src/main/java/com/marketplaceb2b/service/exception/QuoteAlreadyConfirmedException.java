package com.marketplaceb2b.service.exception;

public class QuoteAlreadyConfirmedException extends RuntimeException {

    public QuoteAlreadyConfirmedException(long quoteId) {
        super("Quote was already confirmed under another idempotency key: " + quoteId);
    }
}
