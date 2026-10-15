package com.marketplaceb2b.service;

public class QuoteNotFoundException extends RuntimeException {

    public QuoteNotFoundException(long quoteId) {
        super("Quote not found for this tenant: " + quoteId);
    }
}
