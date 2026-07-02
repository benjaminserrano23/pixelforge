package com.pixelforge.app.game;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameRepository extends JpaRepository<Game, Long> {

    Page<Game> findByDeveloperId(Long developerId, Pageable pageable);

    long countByDeveloperId(Long developerId);

    long countByDeveloperIdAndStatus(Long developerId, GameStatus status);

    // Catálogo público: solo PUBLISHED, con filtros opcionales por género y
    // búsqueda de texto en el título. Los parámetros nulos se ignoran (JPQL
    // corto-circuita la condición cuando ?1/?2 es null) en vez de armar la
    // query dinámicamente con Specifications, que sería sobre-ingeniería
    // para dos filtros simples.
    //
    // El "cast(:search as string)" es necesario: sin él, Postgres no puede
    // inferir el tipo del parámetro cuando llega null (lo asume bytea) y
    // falla con "function lower(bytea) does not exist". H2 (usado en los
    // tests) no tiene este problema, así que solo se manifestó contra Postgres real.
    @Query("""
            select g from Game g
            where g.status = com.pixelforge.app.game.GameStatus.PUBLISHED
              and (:genre is null or g.genre = cast(:genre as string))
              and (:search is null or lower(g.title) like lower(concat('%', cast(:search as string), '%')))
            """)
    Page<Game> findPublished(@Param("genre") String genre, @Param("search") String search, Pageable pageable);
}
