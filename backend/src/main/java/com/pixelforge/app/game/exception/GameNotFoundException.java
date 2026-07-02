package com.pixelforge.app.game.exception;

public class GameNotFoundException extends RuntimeException {
    public GameNotFoundException(Long id) {
        super("game not found: " + id);
    }
}
