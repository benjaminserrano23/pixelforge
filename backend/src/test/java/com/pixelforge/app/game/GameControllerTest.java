package com.pixelforge.app.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pixelforge.app.auth.exception.GlobalExceptionHandler;
import com.pixelforge.app.auth.jwt.JwtService;
import com.pixelforge.app.game.dto.GameRequest;
import com.pixelforge.app.game.dto.GameResponse;
import com.pixelforge.app.game.dto.PageResponse;
import com.pixelforge.app.game.exception.GameNotFoundException;
import com.pixelforge.app.game.exception.NotGameOwnerException;
import com.pixelforge.app.purchase.PurchaseService;
import com.pixelforge.app.purchase.dto.PurchaseResponse;
import com.pixelforge.app.purchase.exception.AlreadyPurchasedException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice del controller con MockMvc, mismo patrón que AuthControllerTest:
 * addFilters=false para no ejercitar Spring Security aquí (eso lo cubre
 * SecurityConfig en manual/E2E), foco en el mapeo HTTP y de excepciones.
 * La autenticación se simula con .principal(...) en el request builder (el
 * argumento Authentication del controller lo resuelve Spring MVC desde
 * request.getUserPrincipal(), sin necesitar la cadena de filtros real).
 */
@WebMvcTest(controllers = GameController.class,
            excludeAutoConfiguration = SecurityAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GameControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean GameService gameService;
    @MockBean PurchaseService purchaseService;
    // Necesario porque @WebMvcTest escanea JwtAuthenticationFilter (Filter), que depende de JwtService.
    @MockBean JwtService jwtService;

    private static final org.springframework.security.core.Authentication DEVELOPER_AUTH =
            new UsernamePasswordAuthenticationToken("dev@example.com", null, List.of());
    private static final org.springframework.security.core.Authentication PLAYER_AUTH =
            new UsernamePasswordAuthenticationToken("player@example.com", null, List.of());

    @Test
    void catalog_returns_published_games() throws Exception {
        var page = new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1);
        when(gameService.findPublished(eq(null), eq(null), any())).thenReturn(page);

        mockMvc.perform(get("/api/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Cave Story"))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void detail_returns_404_when_game_not_found_or_not_published() throws Exception {
        when(gameService.findPublishedById(999L)).thenThrow(new GameNotFoundException(999L));

        mockMvc.perform(get("/api/games/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("game_not_found"));
    }

    @Test
    void create_returns_created_game_as_draft() throws Exception {
        var req = new GameRequest("Cave Story", "Metroidvania", "Platformer", new BigDecimal("9.99"), null);
        when(gameService.create(eq("dev@example.com"), any(GameRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/games")
                        .principal(DEVELOPER_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void create_returns_400_when_title_is_blank() throws Exception {
        String body = """
                {"title": "", "description": "d", "genre": "g", "price": 1.0}
                """;

        mockMvc.perform(post("/api/games")
                        .principal(DEVELOPER_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.title").exists());
    }

    @Test
    void update_returns_403_when_not_owner() throws Exception {
        var req = new GameRequest("X", "d", "g", BigDecimal.ONE, null);
        when(gameService.update(eq("dev@example.com"), eq(10L), any(GameRequest.class)))
                .thenThrow(new NotGameOwnerException(10L));

        mockMvc.perform(put("/api/games/10")
                        .principal(DEVELOPER_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("not_game_owner"));
    }

    @Test
    void delete_returns_200_when_owner() throws Exception {
        mockMvc.perform(delete("/api/games/10")
                        .principal(DEVELOPER_AUTH))
                .andExpect(status().isOk());
    }

    @Test
    void mine_returns_developers_games() throws Exception {
        var page = new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1);
        when(gameService.findMine(eq("dev@example.com"), any())).thenReturn(page);

        mockMvc.perform(get("/api/games/mine")
                        .principal(DEVELOPER_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Cave Story"));
    }

    @Test
    void mineDetail_returns_draft_game_for_its_owner() throws Exception {
        when(gameService.findOwned("dev@example.com", 10L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/games/mine/10")
                        .principal(DEVELOPER_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void purchase_returns_purchase_response() throws Exception {
        var purchaseResponse = new PurchaseResponse(100L, sampleResponse(), new BigDecimal("9.99"),
                Instant.parse("2026-07-02T00:00:00Z"));
        when(purchaseService.purchase("player@example.com", 10L)).thenReturn(purchaseResponse);

        mockMvc.perform(post("/api/games/10/purchase")
                        .principal(PLAYER_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.game.title").value("Cave Story"));
    }

    @Test
    void purchase_returns_409_when_already_purchased() throws Exception {
        when(purchaseService.purchase("player@example.com", 10L))
                .thenThrow(new AlreadyPurchasedException(10L));

        mockMvc.perform(post("/api/games/10/purchase")
                        .principal(PLAYER_AUTH))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("already_purchased"));
    }

    private GameResponse sampleResponse() {
        return new GameResponse(10L, "Cave Story", "Metroidvania", "Platformer", new BigDecimal("9.99"),
                null, GameStatus.DRAFT, 1L, Instant.parse("2026-07-02T00:00:00Z"), Instant.parse("2026-07-02T00:00:00Z"));
    }
}
