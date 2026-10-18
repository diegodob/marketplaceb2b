package com.marketplaceb2b.dto;

import java.util.Map;

/**
 * The stable error contract: a machine-readable {@code code} the client keys on, a clear
 * {@code message} in business language, and optional safe {@code details} (never internal ids or
 * exception text).
 */
public record ApiError(
        String code,
        String message,
        Map<String, String> details) {
}
