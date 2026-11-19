package com.teame.bingo.domain.recognition.dto;

import java.util.List;

import com.teame.bingo.domain.item.dto.ItemSummaryResponse;

public record RecognitionMatchResult(
        boolean matched,
        String label,
        double score,
        List<ItemSummaryResponse> candidates) {

    public static RecognitionMatchResult matched(String label, double score, List<ItemSummaryResponse> candidates) {
        return new RecognitionMatchResult(true, label, score, candidates);
    }

    public static RecognitionMatchResult unmatched(String label, double score) {
        return new RecognitionMatchResult(false, label, score, List.of());
    }
}
