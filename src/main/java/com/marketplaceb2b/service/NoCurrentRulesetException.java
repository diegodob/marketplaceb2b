package com.marketplaceb2b.service;

public class NoCurrentRulesetException extends RuntimeException {

    public NoCurrentRulesetException(long tenantId) {
        super("No ruleset in force for tenant: " + tenantId);
    }
}
