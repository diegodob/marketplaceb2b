package com.marketplaceb2b.service;

public record OrderConfirmation(long orderId, String publicNumber, boolean replayed) {

    public static OrderConfirmation confirmed(long orderId, String publicNumber) {
        return new OrderConfirmation(orderId, publicNumber, false);
    }

    public static OrderConfirmation replay(long orderId, String publicNumber) {
        return new OrderConfirmation(orderId, publicNumber, true);
    }
}
