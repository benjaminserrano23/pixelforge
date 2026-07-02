package com.pixelforge.app.game.dto;

import com.pixelforge.app.game.Game;
import com.pixelforge.app.game.GameStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record GameResponse(
        Long id,
        String title,
        String description,
        String genre,
        BigDecimal price,
        String coverImageUrl,
        GameStatus status,
        Long developerId,
        Instant createdAt,
        Instant updatedAt
) {
    public static GameResponse from(Game game) {
        return new GameResponse(
                game.getId(),
                game.getTitle(),
                game.getDescription(),
                game.getGenre(),
                game.getPrice(),
                game.getCoverImageUrl(),
                game.getStatus(),
                game.getDeveloper().getId(),
                game.getCreatedAt(),
                game.getUpdatedAt()
        );
    }
}
