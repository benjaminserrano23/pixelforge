package com.pixelforge.app.game.exception;

// El DEVELOPER autenticado existe y el juego existe, pero no son dueños uno
// del otro. Se mapea a 403 (no 404): distinguir "no existe" de "no es tuyo"
// no filtra nada sensible aquí, a diferencia de auth donde sí se oculta
// deliberadamente si un email existe.
public class NotGameOwnerException extends RuntimeException {
    public NotGameOwnerException(Long gameId) {
        super("not the owner of game: " + gameId);
    }
}
