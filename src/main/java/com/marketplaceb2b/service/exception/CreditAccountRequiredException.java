package com.marketplaceb2b.service.exception;

public class CreditAccountRequiredException extends RuntimeException {

    public CreditAccountRequiredException(long customerId) {
        super("The payment method uses credit but the customer has no credit account: " + customerId);
    }
}
