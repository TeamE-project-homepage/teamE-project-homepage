package com.teame.bingo.domain.activity.service;

import java.util.List;
import java.util.UUID;

import com.teame.bingo.domain.activity.converter.SavedGuideConverter;
import com.teame.bingo.domain.activity.dto.SavedGuideResponse;
import com.teame.bingo.domain.activity.dto.SavedGuideStatusResponse;
import com.teame.bingo.domain.activity.repository.SavedGuideRepository;
import com.teame.bingo.domain.item.repository.ItemRepository;
import com.teame.bingo.domain.member.repository.MemberRepository;
import com.teame.bingo.global.exception.CustomException;
import com.teame.bingo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SavedGuideService {

    private final SavedGuideRepository savedGuideRepository;
    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;

    public List<SavedGuideResponse> getSavedGuides(UUID memberId) {
        return savedGuideRepository.findAllByMemberIdOrderByCreatedAtDescIdDesc(memberId).stream()
                .map(SavedGuideConverter::toSavedGuideResponse)
                .toList();
    }

    @Transactional
    public SavedGuideStatusResponse saveGuide(UUID memberId, Long itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new CustomException(ErrorCode.ITEM_NOT_FOUND);
        }
        memberRepository.insertIfAbsent(memberId);
        savedGuideRepository.insertIfAbsent(memberId, itemId);
        return new SavedGuideStatusResponse(itemId, true);
    }

    @Transactional
    public SavedGuideStatusResponse deleteSavedGuide(UUID memberId, Long itemId) {
        savedGuideRepository.deleteByMemberIdAndItemId(memberId, itemId);
        return new SavedGuideStatusResponse(itemId, false);
    }
}
