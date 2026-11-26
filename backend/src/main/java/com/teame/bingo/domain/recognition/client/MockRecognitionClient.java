package com.teame.bingo.domain.recognition.client;

import com.teame.bingo.domain.recognition.dto.RecognitionPrediction;
import org.springframework.http.MediaType;

public class MockRecognitionClient implements RecognitionClient {

    @Override
    public RecognitionPrediction predict(byte[] image, MediaType contentType) {
        return new RecognitionPrediction("mock", "plastic", 0.91);
    }
}
