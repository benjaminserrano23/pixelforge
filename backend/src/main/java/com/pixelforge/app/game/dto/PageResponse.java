package com.pixelforge.app.game.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

// Envoltorio explícito en vez de exponer org.springframework.data.domain.Page
// directamente: Page serializa con campos internos de Spring Data (pageable,
// sort, etc.) que no queremos comprometernos a mantener en el contrato de la API.
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
