package com.pixelforge.app.purchase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    Page<Purchase> findByUserId(Long userId, Pageable pageable);

    boolean existsByUserIdAndGameId(Long userId, Long gameId);

    // left join: incluye juegos sin ninguna compra (0 adquisiciones, 0 en
    // ingresos) para que el dashboard muestre todos los juegos del
    // desarrollador, no solo los que ya vendieron algo.
    @Query("""
            select g.id as gameId, g.title as title,
                   count(p.id) as purchases,
                   coalesce(sum(p.amount), 0) as revenue
            from Game g left join Purchase p on p.game = g
            where g.developer.id = :developerId
            group by g.id, g.title
            order by g.id
            """)
    List<GameStatsProjection> statsByDeveloper(@Param("developerId") Long developerId);
}
