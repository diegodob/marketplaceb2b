package com.marketplaceb2b.service.exception;

public class QuoteNotFoundException extends RuntimeException {

    public QuoteNotFoundException(long quoteId) {
        super("Quote not found for this tenant: " + quoteId);
    }

    public QuoteNotFoundException(String reference) {
        super("Quote not found for this tenant: " + reference);
    }
}
