package com.pixelforge.app.purchase.dto;

import java.math.BigDecimal;
import java.util.List;

public record StatsOverviewResponse(
        long totalGames,
        long publishedGames,
        long totalPurchases,
        BigDecimal totalRevenue,
        List<GameStatsEntry> perGame
) {}
