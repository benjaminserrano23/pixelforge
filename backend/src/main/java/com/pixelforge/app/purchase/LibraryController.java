package com.pixelforge.app.purchase;

import com.pixelforge.app.game.dto.PageResponse;
import com.pixelforge.app.purchase.dto.PurchaseResponse;

import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Jugador (rol PLAYER, ver SecurityConfig): juegos ya adquiridos.
@RestController
@RequestMapping("/api/library")
public class LibraryController {

    private final PurchaseService purchaseService;

    public LibraryController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @GetMapping
    public PageResponse<PurchaseResponse> library(Authentication authentication, Pageable pageable) {
        return purchaseService.findLibrary(authentication.getName(), pageable);
    }
}
