package com.teame.bingo.domain.item.dto;

import java.util.List;

public record SearchResponse(String query, int totalCount, List<SearchItemResponse> items) {
}
