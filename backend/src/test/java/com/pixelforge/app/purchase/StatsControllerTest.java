package com.pixelforge.app.purchase;

import com.pixelforge.app.auth.exception.GlobalExceptionHandler;
import com.pixelforge.app.auth.jwt.JwtService;
import com.pixelforge.app.purchase.dto.GameStatsEntry;
import com.pixelforge.app.purchase.dto.StatsOverviewResponse;

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
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StatsController.class,
            excludeAutoConfiguration = SecurityAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StatsControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean PurchaseService purchaseService;
    @MockBean JwtService jwtService;

    private static final Authentication DEVELOPER_AUTH =
            new UsernamePasswordAuthenticationToken("dev@example.com", null, List.of());

    @Test
    void overview_returns_totals_and_per_game_breakdown() throws Exception {
        var response = new StatsOverviewResponse(3, 2, 5, new BigDecimal("49.95"),
                List.of(new GameStatsEntry(10L, "Cave Story", 5, new BigDecimal("49.95"))));
        when(purchaseService.statsOverview("dev@example.com")).thenReturn(response);

        mockMvc.perform(get("/api/stats/overview")
                        .principal(DEVELOPER_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGames").value(3))
                .andExpect(jsonPath("$.perGame[0].title").value("Cave Story"));
    }
}
