package com.teame.bingo.domain.recognition.client;

import com.teame.bingo.domain.recognition.dto.RecognitionPrediction;
import org.springframework.http.MediaType;

public interface RecognitionClient {

    RecognitionPrediction predict(byte[] image, MediaType contentType);
}
