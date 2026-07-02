package com.pixelforge.app.purchase.dto;

import com.pixelforge.app.game.dto.GameResponse;
import com.pixelforge.app.purchase.Purchase;

import java.math.BigDecimal;
import java.time.Instant;

// La biblioteca necesita los datos del juego (título, portada, precio) para
// renderizar tarjetas, así que la respuesta incluye el GameResponse completo
// en vez de solo el gameId.
public record PurchaseResponse(
        Long id,
        GameResponse game,
        BigDecimal amount,
        Instant createdAt
) {
    public static PurchaseResponse from(Purchase purchase) {
        return new PurchaseResponse(
                purchase.getId(),
                GameResponse.from(purchase.getGame()),
                purchase.getAmount(),
                purchase.getCreatedAt()
        );
    }
}
