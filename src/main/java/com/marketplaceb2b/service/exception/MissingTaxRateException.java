package com.marketplaceb2b.service.exception;

public class MissingTaxRateException extends RuntimeException {

    public MissingTaxRateException(long rulesetId, long taxCategoryId) {
        super("Tax category " + taxCategoryId + " has no rate in ruleset " + rulesetId);
    }
}
