package com.marketplaceb2b.service.exception;

import java.math.BigDecimal;

public class InsufficientCreditException extends RuntimeException {

    public InsufficientCreditException(long creditAccountId, BigDecimal available, BigDecimal required) {
        super("Insufficient credit on account " + creditAccountId + ": available " + available + ", required " + required);
    }
}
