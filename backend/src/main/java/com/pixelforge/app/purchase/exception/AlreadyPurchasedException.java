package com.pixelforge.app.purchase.exception;

public class AlreadyPurchasedException extends RuntimeException {
    public AlreadyPurchasedException(Long gameId) {
        super("game already purchased: " + gameId);
    }
}
