package com.tradeblotter.exception;

public class TradeNotFoundException extends RuntimeException {

    public TradeNotFoundException(Long id) {
        super("Trade not found: " + id);
    }
}
