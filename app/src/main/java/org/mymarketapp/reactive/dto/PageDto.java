package org.mymarketapp.reactive.dto;

public record PageDto(
        int pageSize,
        int pageNumber,
        boolean hasPrevious,
        boolean hasNext
) {}
