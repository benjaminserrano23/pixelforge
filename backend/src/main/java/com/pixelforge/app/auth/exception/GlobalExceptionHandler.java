package com.pixelforge.app.auth.exception;

import com.pixelforge.app.game.CoverStorageService;
import com.pixelforge.app.game.exception.GameNotFoundException;
import com.pixelforge.app.game.exception.NotGameOwnerException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * @Valid en un @RequestBody falla -> 400 con detalle por campo.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.put(fe.getField(),
                        fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "validation_failed");
        body.put("fields", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "invalid_credentials"));
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ResponseEntity<Map<String, String>> handleEmailUsed(EmailAlreadyUsedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "email_already_used"));
    }

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleGameNotFound(GameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "game_not_found"));
    }

    @ExceptionHandler(NotGameOwnerException.class)
    public ResponseEntity<Map<String, String>> handleNotGameOwner(NotGameOwnerException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "not_game_owner"));
    }

    @ExceptionHandler(CoverStorageService.InvalidCoverException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCover(CoverStorageService.InvalidCoverException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "invalid_cover", "message", ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "invalid_cover", "message", "la imagen supera el máximo permitido"));
    }
}
