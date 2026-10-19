package com.marketplaceb2b.service.exception;

public class MissingCustomerException extends RuntimeException {

    public MissingCustomerException(long tenantId, Long customerId) {
        super("Customer " + customerId + " not found for tenant " + tenantId);
    }
}
