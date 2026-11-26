package com.teame.bingo.domain.recognition.service;

import com.teame.bingo.domain.recognition.client.RecognitionClient;
import com.teame.bingo.domain.recognition.dto.RecognitionMatchResult;
import com.teame.bingo.domain.recognition.dto.RecognitionPrediction;
import com.teame.bingo.global.exception.CustomException;
import com.teame.bingo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class RecognitionService {

    private final RecognitionImageValidator imageValidator;
    private final RecognitionClient recognitionClient;
    private final RecognitionMatcher recognitionMatcher;

    // The remote call runs before the matcher's read-only DB transaction starts.
    public RecognitionMatchResult recognize(MultipartFile file) {
        RecognitionImageValidator.ValidatedImage image = imageValidator.validate(file);
        RecognitionPrediction prediction = recognitionClient.predict(image.bytes(), image.contentType());
        if (prediction == null || !StringUtils.hasText(prediction.model()) || !StringUtils.hasText(prediction.label())
                || prediction.score() == null || !Double.isFinite(prediction.score())
                || prediction.score() < 0 || prediction.score() > 1) {
            throw new CustomException(ErrorCode.RECOGNITION_UNAVAILABLE);
        }
        return recognitionMatcher.match(prediction.label(), prediction.score());
    }
}
