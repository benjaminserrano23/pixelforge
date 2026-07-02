package com.pixelforge.app.game;

import com.pixelforge.app.game.dto.GameRequest;
import com.pixelforge.app.game.dto.GameResponse;
import com.pixelforge.app.game.dto.PageResponse;
import com.pixelforge.app.game.exception.GameNotFoundException;
import com.pixelforge.app.game.exception.NotGameOwnerException;
import com.pixelforge.app.user.User;
import com.pixelforge.app.user.UserRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class GameService {

    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final CoverStorageService coverStorageService;

    public GameService(GameRepository gameRepository,
                       UserRepository userRepository,
                       CoverStorageService coverStorageService) {
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
        this.coverStorageService = coverStorageService;
    }

    @Transactional(readOnly = true)
    public PageResponse<GameResponse> findPublished(String genre, String search, Pageable pageable) {
        return PageResponse.from(gameRepository.findPublished(genre, search, pageable), GameResponse::from);
    }

    @Transactional(readOnly = true)
    public GameResponse findPublishedById(Long id) {
        Game game = gameRepository.findById(id)
                .filter(g -> g.getStatus() == GameStatus.PUBLISHED)
                .orElseThrow(() -> new GameNotFoundException(id));
        return GameResponse.from(game);
    }

    @Transactional(readOnly = true)
    public PageResponse<GameResponse> findMine(String developerEmail, Pageable pageable) {
        User developer = requireUser(developerEmail);
        return PageResponse.from(gameRepository.findByDeveloperId(developer.getId(), pageable), GameResponse::from);
    }

    @Transactional
    public GameResponse create(String developerEmail, GameRequest req) {
        User developer = requireUser(developerEmail);
        Game game = new Game();
        game.setDeveloper(developer);
        applyFields(game, req);
        // Un juego siempre nace DRAFT: publicarlo es una acción explícita
        // posterior (PUT con status=PUBLISHED), no un efecto secundario del alta.
        game.setStatus(GameStatus.DRAFT);
        return GameResponse.from(gameRepository.save(game));
    }

    @Transactional
    public GameResponse update(String developerEmail, Long id, GameRequest req) {
        Game game = requireOwnedGame(developerEmail, id);
        applyFields(game, req);
        if (req.status() != null) {
            game.setStatus(req.status());
        }
        return GameResponse.from(gameRepository.save(game));
    }

    @Transactional
    public void delete(String developerEmail, Long id) {
        Game game = requireOwnedGame(developerEmail, id);
        gameRepository.delete(game);
    }

    @Transactional
    public GameResponse uploadCover(String developerEmail, Long id, MultipartFile file) {
        Game game = requireOwnedGame(developerEmail, id);
        game.setCoverImageUrl(coverStorageService.store(file));
        return GameResponse.from(gameRepository.save(game));
    }

    private void applyFields(Game game, GameRequest req) {
        game.setTitle(req.title());
        game.setDescription(req.description());
        game.setGenre(req.genre());
        game.setPrice(req.price());
    }

    private Game requireOwnedGame(String developerEmail, Long id) {
        User developer = requireUser(developerEmail);
        Game game = gameRepository.findById(id).orElseThrow(() -> new GameNotFoundException(id));
        if (!game.getDeveloper().getId().equals(developer.getId())) {
            throw new NotGameOwnerException(id);
        }
        return game;
    }

    private User requireUser(String email) {
        // El JWT ya fue validado por el filtro; si el email no resuelve a un
        // usuario es porque fue borrado tras emitirse el token.
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("user not found"));
    }
}
