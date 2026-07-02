package com.pixelforge.app.purchase;

import com.pixelforge.app.game.Game;
import com.pixelforge.app.game.GameRepository;
import com.pixelforge.app.game.GameStatus;
import com.pixelforge.app.game.dto.PageResponse;
import com.pixelforge.app.game.exception.GameNotFoundException;
import com.pixelforge.app.purchase.dto.PurchaseResponse;
import com.pixelforge.app.purchase.dto.StatsOverviewResponse;
import com.pixelforge.app.purchase.exception.AlreadyPurchasedException;
import com.pixelforge.app.user.User;
import com.pixelforge.app.user.UserRepository;
import com.pixelforge.app.user.UserRole;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock PurchaseRepository purchaseRepository;
    @Mock GameRepository gameRepository;
    @Mock UserRepository userRepository;

    @InjectMocks PurchaseService purchaseService;

    @Test
    void purchase_creates_record_with_price_snapshot() {
        User player = player();
        Game game = publishedGame();
        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(player));
        when(gameRepository.findById(10L)).thenReturn(Optional.of(game));
        when(purchaseRepository.existsByUserIdAndGameId(1L, 10L)).thenReturn(false);
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(inv -> {
            Purchase p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });

        PurchaseResponse res = purchaseService.purchase("player@example.com", 10L);

        assertThat(res.amount()).isEqualByComparingTo("9.99");
        assertThat(res.game().id()).isEqualTo(10L);
    }

    @Test
    void purchase_throws_when_already_purchased() {
        User player = player();
        Game game = publishedGame();
        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(player));
        when(gameRepository.findById(10L)).thenReturn(Optional.of(game));
        when(purchaseRepository.existsByUserIdAndGameId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> purchaseService.purchase("player@example.com", 10L))
                .isInstanceOf(AlreadyPurchasedException.class);
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void purchase_throws_not_found_for_draft_game() {
        User player = player();
        Game draft = publishedGame();
        draft.setStatus(GameStatus.DRAFT);
        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(player));
        when(gameRepository.findById(10L)).thenReturn(Optional.of(draft));

        // Igual que el catálogo público: un DRAFT es indistinguible de "no existe".
        assertThatThrownBy(() -> purchaseService.purchase("player@example.com", 10L))
                .isInstanceOf(GameNotFoundException.class);
        verify(purchaseRepository, never()).existsByUserIdAndGameId(any(), any());
    }

    @Test
    void findLibrary_maps_page_from_repository() {
        User player = player();
        Purchase purchase = new Purchase();
        purchase.setId(100L);
        purchase.setGame(publishedGame());
        purchase.setUser(player);
        purchase.setAmount(new BigDecimal("9.99"));
        var page = new PageImpl<>(List.of(purchase), PageRequest.of(0, 20), 1);

        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(player));
        when(purchaseRepository.findByUserId(1L, PageRequest.of(0, 20))).thenReturn(page);

        PageResponse<PurchaseResponse> res = purchaseService.findLibrary("player@example.com", PageRequest.of(0, 20));

        assertThat(res.items()).hasSize(1);
        assertThat(res.items().get(0).game().title()).isEqualTo("Cave Story");
    }

    @Test
    void statsOverview_aggregates_totals_and_per_game_breakdown() {
        User developer = developer();
        when(userRepository.findByEmail("dev@example.com")).thenReturn(Optional.of(developer));
        when(gameRepository.countByDeveloperId(1L)).thenReturn(3L);
        when(gameRepository.countByDeveloperIdAndStatus(1L, GameStatus.PUBLISHED)).thenReturn(2L);
        when(purchaseRepository.statsByDeveloper(1L)).thenReturn(List.of(
                projection(10L, "Cave Story", 3L, new BigDecimal("29.97")),
                projection(11L, "Nebula Drift", 0L, BigDecimal.ZERO)
        ));

        StatsOverviewResponse res = purchaseService.statsOverview("dev@example.com");

        assertThat(res.totalGames()).isEqualTo(3);
        assertThat(res.publishedGames()).isEqualTo(2);
        assertThat(res.totalPurchases()).isEqualTo(3);
        assertThat(res.totalRevenue()).isEqualByComparingTo("29.97");
        assertThat(res.perGame()).hasSize(2);
    }

    private GameStatsProjection projection(Long gameId, String title, long purchases, BigDecimal revenue) {
        return new GameStatsProjection() {
            public Long getGameId() { return gameId; }
            public String getTitle() { return title; }
            public Long getPurchases() { return purchases; }
            public BigDecimal getRevenue() { return revenue; }
        };
    }

    private User player() {
        User u = new User();
        u.setId(1L);
        u.setEmail("player@example.com");
        u.setDisplayName("Player");
        u.setRole(UserRole.PLAYER);
        return u;
    }

    private User developer() {
        User u = new User();
        u.setId(1L);
        u.setEmail("dev@example.com");
        u.setDisplayName("Dev");
        u.setRole(UserRole.DEVELOPER);
        return u;
    }

    private Game publishedGame() {
        Game g = new Game();
        g.setId(10L);
        g.setTitle("Cave Story");
        g.setDescription("Metroidvania");
        g.setGenre("Platformer");
        g.setPrice(new BigDecimal("9.99"));
        g.setStatus(GameStatus.PUBLISHED);
        g.setDeveloper(developer());
        return g;
    }
}
