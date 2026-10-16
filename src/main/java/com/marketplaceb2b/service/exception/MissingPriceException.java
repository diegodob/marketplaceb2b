package com.marketplaceb2b.service.exception;

public class MissingPriceException extends RuntimeException {

    public MissingPriceException(long rulesetId, long productId) {
        super("Product " + productId + " has no price in ruleset " + rulesetId);
    }
}
