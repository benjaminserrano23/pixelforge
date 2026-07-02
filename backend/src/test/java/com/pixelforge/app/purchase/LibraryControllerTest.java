package com.pixelforge.app.purchase;

import com.pixelforge.app.auth.exception.GlobalExceptionHandler;
import com.pixelforge.app.auth.jwt.JwtService;
import com.pixelforge.app.game.GameStatus;
import com.pixelforge.app.game.dto.GameResponse;
import com.pixelforge.app.game.dto.PageResponse;
import com.pixelforge.app.purchase.dto.PurchaseResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LibraryController.class,
            excludeAutoConfiguration = SecurityAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LibraryControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean PurchaseService purchaseService;
    @MockBean JwtService jwtService;

    private static final Authentication PLAYER_AUTH =
            new UsernamePasswordAuthenticationToken("player@example.com", null, List.of());

    @Test
    void library_returns_purchased_games() throws Exception {
        var game = new GameResponse(10L, "Cave Story", "Metroidvania", "Platformer", new BigDecimal("9.99"),
                null, GameStatus.PUBLISHED, 1L, Instant.parse("2026-07-02T00:00:00Z"), Instant.parse("2026-07-02T00:00:00Z"));
        var purchase = new PurchaseResponse(100L, game, new BigDecimal("9.99"), Instant.parse("2026-07-02T00:00:00Z"));
        var page = new PageResponse<>(List.of(purchase), 0, 20, 1, 1);
        when(purchaseService.findLibrary(eq("player@example.com"), any())).thenReturn(page);

        mockMvc.perform(get("/api/library")
                        .principal(PLAYER_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].game.title").value("Cave Story"));
    }
}
