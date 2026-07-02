package com.pixelforge.app.purchase;

import java.math.BigDecimal;

// Proyección por interfaz: Spring Data mapea los alias de la @Query a estos
// getters. Se prefiere sobre una expresión "select new ..." de JPQL porque
// el soporte de Hibernate para constructores de record en "select new" es
// más frágil que las proyecciones de interfaz, que llevan años estables.
public interface GameStatsProjection {
    Long getGameId();
    String getTitle();
    Long getPurchases();
    BigDecimal getRevenue();
}
