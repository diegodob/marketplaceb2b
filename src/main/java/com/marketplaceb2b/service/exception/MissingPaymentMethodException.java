package com.marketplaceb2b.service.exception;

public class MissingPaymentMethodException extends RuntimeException {

    public MissingPaymentMethodException(long tenantId, Long paymentMethodId) {
        super("Payment method " + paymentMethodId + " not found for tenant " + tenantId);
    }
}
