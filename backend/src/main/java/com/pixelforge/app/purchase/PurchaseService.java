package com.pixelforge.app.purchase;

import com.pixelforge.app.game.Game;
import com.pixelforge.app.game.GameRepository;
import com.pixelforge.app.game.GameStatus;
import com.pixelforge.app.game.dto.PageResponse;
import com.pixelforge.app.game.exception.GameNotFoundException;
import com.pixelforge.app.purchase.dto.GameStatsEntry;
import com.pixelforge.app.purchase.dto.PurchaseResponse;
import com.pixelforge.app.purchase.dto.StatsOverviewResponse;
import com.pixelforge.app.purchase.exception.AlreadyPurchasedException;
import com.pixelforge.app.user.User;
import com.pixelforge.app.user.UserRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    public PurchaseService(PurchaseRepository purchaseRepository,
                           GameRepository gameRepository,
                           UserRepository userRepository) {
        this.purchaseRepository = purchaseRepository;
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PurchaseResponse purchase(String playerEmail, Long gameId) {
        User player = requireUser(playerEmail);

        // Mismo tratamiento que el catálogo público: un juego que no existe o
        // que todavía es DRAFT es indistinguible desde afuera (404), para no
        // filtrar juegos no publicados.
        Game game = gameRepository.findById(gameId)
                .filter(g -> g.getStatus() == GameStatus.PUBLISHED)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        if (purchaseRepository.existsByUserIdAndGameId(player.getId(), gameId)) {
            throw new AlreadyPurchasedException(gameId);
        }

        Purchase purchase = new Purchase();
        purchase.setGame(game);
        purchase.setUser(player);
        // Snapshot del precio al momento de la compra: si el desarrollador
        // cambia el precio después, los ingresos históricos no se mueven.
        purchase.setAmount(game.getPrice());

        return PurchaseResponse.from(purchaseRepository.save(purchase));
    }

    @Transactional(readOnly = true)
    public PageResponse<PurchaseResponse> findLibrary(String playerEmail, Pageable pageable) {
        User player = requireUser(playerEmail);
        return PageResponse.from(purchaseRepository.findByUserId(player.getId(), pageable), PurchaseResponse::from);
    }

    @Transactional(readOnly = true)
    public StatsOverviewResponse statsOverview(String developerEmail) {
        User developer = requireUser(developerEmail);

        var perGame = purchaseRepository.statsByDeveloper(developer.getId()).stream()
                .map(p -> new GameStatsEntry(p.getGameId(), p.getTitle(), p.getPurchases(), p.getRevenue()))
                .toList();

        long totalPurchases = perGame.stream().mapToLong(GameStatsEntry::purchases).sum();
        BigDecimal totalRevenue = perGame.stream()
                .map(GameStatsEntry::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new StatsOverviewResponse(
                gameRepository.countByDeveloperId(developer.getId()),
                gameRepository.countByDeveloperIdAndStatus(developer.getId(), GameStatus.PUBLISHED),
                totalPurchases,
                totalRevenue,
                perGame
        );
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("user not found"));
    }
}
