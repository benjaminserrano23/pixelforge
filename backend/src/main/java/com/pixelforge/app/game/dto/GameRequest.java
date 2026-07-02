package com.pixelforge.app.game.dto;

import com.pixelforge.app.game.GameStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// Mismo DTO para crear y actualizar: en creación "status" se ignora (siempre
// nace DRAFT); en edición permite publicar/despublicar. Repetir el DTO para
// "create" y "update" no aportaría nada, solo duplicaría los campos.
public record GameRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 2000) String description,
        @NotBlank @Size(max = 60) String genre,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal price,
        GameStatus status
) {}
