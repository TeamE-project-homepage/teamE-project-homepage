package com.teame.bingo.domain.activity.dto;

import java.time.OffsetDateTime;

public record SavedGuideResponse(Long savedGuideId, OffsetDateTime savedAt, SavedGuideItemResponse item) {
}
