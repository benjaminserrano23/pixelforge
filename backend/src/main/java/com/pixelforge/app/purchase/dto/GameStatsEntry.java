package com.pixelforge.app.purchase.dto;

import java.math.BigDecimal;

// Una fila del gráfico de stats: un juego del desarrollador con sus
// adquisiciones e ingresos. Se arma a mano desde GameStatsProjection en vez
// de exponer la proyección directamente, para no acoplar el contrato HTTP
// a la forma exacta de la query de Hibernate.
public record GameStatsEntry(
        Long gameId,
        String title,
        long purchases,
        BigDecimal revenue
) {}
