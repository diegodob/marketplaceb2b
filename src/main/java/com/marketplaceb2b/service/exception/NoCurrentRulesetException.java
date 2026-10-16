package com.marketplaceb2b.service.exception;

public class NoCurrentRulesetException extends RuntimeException {

    public NoCurrentRulesetException(long tenantId) {
        super("No ruleset in force for tenant: " + tenantId);
    }
}
