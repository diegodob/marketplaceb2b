package com.marketplaceb2b.dto;

import java.math.BigDecimal;

/** The confirmation response: the client's reference and the stored total. */
public record CreatedOrder(String number, BigDecimal total, boolean replayed) {
}
