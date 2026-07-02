package com.pixelforge.app.game;

import com.pixelforge.app.game.dto.GameRequest;
import com.pixelforge.app.game.dto.GameResponse;
import com.pixelforge.app.game.dto.PageResponse;
import com.pixelforge.app.game.exception.GameNotFoundException;
import com.pixelforge.app.game.exception.NotGameOwnerException;
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
import org.springframework.security.authentication.BadCredentialsException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock GameRepository gameRepository;
    @Mock UserRepository userRepository;
    @Mock CoverStorageService coverStorageService;

    @InjectMocks GameService gameService;

    @Test
    void create_saves_game_as_draft_owned_by_developer() {
        User developer = developer();
        when(userRepository.findByEmail("dev@example.com")).thenReturn(Optional.of(developer));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> {
            Game g = inv.getArgument(0);
            g.setId(10L);
            return g;
        });

        var req = new GameRequest("Cave Story", "Metroidvania", "Platformer", new BigDecimal("9.99"), GameStatus.PUBLISHED);
        GameResponse res = gameService.create("dev@example.com", req);

        // Aunque el request pida PUBLISHED, un juego siempre nace DRAFT:
        // publicarlo es una acción explícita posterior vía update.
        assertThat(res.status()).isEqualTo(GameStatus.DRAFT);
        assertThat(res.title()).isEqualTo("Cave Story");
        assertThat(res.developerId()).isEqualTo(1L);
    }

    @Test
    void update_applies_fields_and_status_when_owner() {
        Game existing = sampleGame(developer());
        when(userRepository.findByEmail("dev@example.com")).thenReturn(Optional.of(developer()));
        when(gameRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        var req = new GameRequest("Cave Story 2", "Sequel", "Platformer", new BigDecimal("14.99"), GameStatus.PUBLISHED);
        GameResponse res = gameService.update("dev@example.com", 10L, req);

        assertThat(res.title()).isEqualTo("Cave Story 2");
        assertThat(res.status()).isEqualTo(GameStatus.PUBLISHED);
    }

    @Test
    void update_throws_when_caller_is_not_the_owner() {
        User owner = developer();
        User someoneElse = new User();
        someoneElse.setId(2L);
        someoneElse.setEmail("other@example.com");
        someoneElse.setRole(UserRole.DEVELOPER);

        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(someoneElse));
        when(gameRepository.findById(10L)).thenReturn(Optional.of(sampleGame(owner)));

        var req = new GameRequest("Hijack", "desc", "genre", BigDecimal.ONE, null);

        assertThatThrownBy(() -> gameService.update("other@example.com", 10L, req))
                .isInstanceOf(NotGameOwnerException.class);
        verify(gameRepository, never()).save(any());
    }

    @Test
    void update_throws_game_not_found_when_id_does_not_exist() {
        when(userRepository.findByEmail("dev@example.com")).thenReturn(Optional.of(developer()));
        when(gameRepository.findById(999L)).thenReturn(Optional.empty());

        var req = new GameRequest("X", "desc", "genre", BigDecimal.ONE, null);

        assertThatThrownBy(() -> gameService.update("dev@example.com", 999L, req))
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void delete_removes_game_when_owner() {
        Game existing = sampleGame(developer());
        when(userRepository.findByEmail("dev@example.com")).thenReturn(Optional.of(developer()));
        when(gameRepository.findById(10L)).thenReturn(Optional.of(existing));

        gameService.delete("dev@example.com", 10L);

        verify(gameRepository).delete(existing);
    }

    @Test
    void findPublishedById_throws_not_found_for_draft_game() {
        Game draft = sampleGame(developer());
        draft.setStatus(GameStatus.DRAFT);
        when(gameRepository.findById(10L)).thenReturn(Optional.of(draft));

        // Un DRAFT no debe ser visible por su id público: se trata igual que
        // "no existe" para no filtrar juegos no publicados.
        assertThatThrownBy(() -> gameService.findPublishedById(10L))
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void findPublishedById_returns_game_when_published() {
        Game published = sampleGame(developer());
        published.setStatus(GameStatus.PUBLISHED);
        when(gameRepository.findById(10L)).thenReturn(Optional.of(published));

        GameResponse res = gameService.findPublishedById(10L);

        assertThat(res.status()).isEqualTo(GameStatus.PUBLISHED);
    }

    @Test
    void findPublished_maps_page_from_repository() {
        Game published = sampleGame(developer());
        published.setStatus(GameStatus.PUBLISHED);
        var page = new PageImpl<>(List.of(published), PageRequest.of(0, 20), 1);
        when(gameRepository.findPublished(eq("Platformer"), eq(null), any())).thenReturn(page);

        PageResponse<GameResponse> res = gameService.findPublished("Platformer", null, PageRequest.of(0, 20));

        assertThat(res.items()).hasSize(1);
        assertThat(res.totalItems()).isEqualTo(1);
        assertThat(res.items().get(0).title()).isEqualTo("Cave Story");
    }

    @Test
    void uploadCover_stores_file_and_sets_url_when_owner() {
        Game existing = sampleGame(developer());
        when(userRepository.findByEmail("dev@example.com")).thenReturn(Optional.of(developer()));
        when(gameRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(coverStorageService.store(any())).thenReturn("/uploads/cover.png");
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        GameResponse res = gameService.uploadCover("dev@example.com", 10L, null);

        assertThat(res.coverImageUrl()).isEqualTo("/uploads/cover.png");
    }

    @Test
    void requireUser_throws_bad_credentials_when_user_was_deleted() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.findMine("ghost@example.com", PageRequest.of(0, 20)))
                .isInstanceOf(BadCredentialsException.class);
    }

    private User developer() {
        User u = new User();
        u.setId(1L);
        u.setEmail("dev@example.com");
        u.setDisplayName("Dev");
        u.setRole(UserRole.DEVELOPER);
        return u;
    }

    private Game sampleGame(User owner) {
        Game g = new Game();
        g.setId(10L);
        g.setTitle("Cave Story");
        g.setDescription("Metroidvania");
        g.setGenre("Platformer");
        g.setPrice(new BigDecimal("9.99"));
        g.setStatus(GameStatus.DRAFT);
        g.setDeveloper(owner);
        return g;
    }
}
