package com.pixelforge.app.purchase;

import com.pixelforge.app.purchase.dto.StatsOverviewResponse;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Desarrollador (rol DEVELOPER, ver SecurityConfig): totales e ingresos de sus propios juegos.
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final PurchaseService purchaseService;

    public StatsController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @GetMapping("/overview")
    public StatsOverviewResponse overview(Authentication authentication) {
        return purchaseService.statsOverview(authentication.getName());
    }
}
