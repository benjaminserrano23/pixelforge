package com.pixelforge.app.game;

import com.pixelforge.app.game.dto.GameRequest;
import com.pixelforge.app.game.dto.GameResponse;
import com.pixelforge.app.game.dto.PageResponse;
import com.pixelforge.app.purchase.PurchaseService;
import com.pixelforge.app.purchase.dto.PurchaseResponse;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;
    private final PurchaseService purchaseService;

    public GameController(GameService gameService, PurchaseService purchaseService) {
        this.gameService = gameService;
        this.purchaseService = purchaseService;
    }

    // Catálogo público — sin auth (ver SecurityConfig). "mine" no puede vivir
    // bajo el mismo path que "/{id}" con un Long, así que se registra antes.
    @GetMapping
    public PageResponse<GameResponse> catalog(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return gameService.findPublished(genre, search, pageable);
    }

    @GetMapping("/{id}")
    public GameResponse detail(@PathVariable Long id) {
        return gameService.findPublishedById(id);
    }

    @GetMapping("/mine")
    public PageResponse<GameResponse> mine(Authentication authentication, Pageable pageable) {
        return gameService.findMine(authentication.getName(), pageable);
    }

    // El formulario de edición necesita ver el juego sin importar su status
    // (uno recién creado es DRAFT); "/{id}" de arriba es el catálogo público,
    // que solo expone PUBLISHED.
    @GetMapping("/mine/{id}")
    public GameResponse mineDetail(Authentication authentication, @PathVariable Long id) {
        return gameService.findOwned(authentication.getName(), id);
    }

    @PostMapping
    public GameResponse create(Authentication authentication, @Valid @RequestBody GameRequest req) {
        return gameService.create(authentication.getName(), req);
    }

    @PutMapping("/{id}")
    public GameResponse update(Authentication authentication, @PathVariable Long id, @Valid @RequestBody GameRequest req) {
        return gameService.update(authentication.getName(), id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(Authentication authentication, @PathVariable Long id) {
        gameService.delete(authentication.getName(), id);
    }

    @PostMapping("/{id}/cover")
    public GameResponse uploadCover(Authentication authentication, @PathVariable Long id,
                                    @RequestPart("file") MultipartFile file) {
        return gameService.uploadCover(authentication.getName(), id, file);
    }

    // Jugador (rol PLAYER, ver SecurityConfig): "adquirir" un juego publicado.
    @PostMapping("/{id}/purchase")
    public PurchaseResponse purchase(Authentication authentication, @PathVariable Long id) {
        return purchaseService.purchase(authentication.getName(), id);
    }
}
