package com.marketplaceb2b.service;

public class QuoteExpiredException extends RuntimeException {

    public QuoteExpiredException(long quoteId) {
        super("Quote has expired and must be re-quoted: " + quoteId);
    }
}
