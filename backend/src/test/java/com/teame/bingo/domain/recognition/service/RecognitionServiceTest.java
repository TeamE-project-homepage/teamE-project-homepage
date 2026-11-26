package com.teame.bingo.domain.recognition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import com.teame.bingo.domain.recognition.RecognitionTestImages;
import com.teame.bingo.domain.recognition.client.MockRecognitionClient;
import com.teame.bingo.domain.recognition.client.RecognitionClient;
import com.teame.bingo.domain.recognition.dto.RecognitionMatchResult;
import com.teame.bingo.domain.recognition.dto.RecognitionPrediction;
import com.teame.bingo.global.exception.CustomException;
import com.teame.bingo.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class RecognitionServiceTest {

    @Spy
    private RecognitionImageValidator imageValidator = new RecognitionImageValidator();

    @Mock
    private RecognitionClient recognitionClient;

    @Mock
    private RecognitionMatcher recognitionMatcher;

    @InjectMocks
    private RecognitionService recognitionService;

    @Test
    void delegatesPredictionToExistingMatcherWithoutChangingResult() throws Exception {
        var expected = RecognitionMatchResult.unmatched("pet_bottle_clear", 0.9);
        given(recognitionClient.predict(any(byte[].class), eq(MediaType.IMAGE_PNG)))
                .willReturn(new RecognitionPrediction("test", "pet_bottle_clear", 0.9));
        given(recognitionMatcher.match("pet_bottle_clear", 0.9)).willReturn(expected);

        assertThat(recognitionService.recognize(photo())).isSameAs(expected);
        verify(recognitionMatcher).match("pet_bottle_clear", 0.9);
    }

    @Test
    void mockModeUsesTheSameMatcherAndResponseContract() throws Exception {
        RecognitionService service = new RecognitionService(imageValidator, new MockRecognitionClient(), recognitionMatcher);
        var expected = RecognitionMatchResult.unmatched("plastic", 0.91);
        given(recognitionMatcher.match("plastic", 0.91)).willReturn(expected);

        assertThat(service.recognize(photo())).isSameAs(expected);
        verifyNoInteractions(recognitionClient);
    }

    @ParameterizedTest
    @MethodSource("invalidPredictions")
    void invalidUpstreamDataReturns503BeforeDatabaseMatching(RecognitionPrediction prediction) throws Exception {
        given(recognitionClient.predict(any(byte[].class), eq(MediaType.IMAGE_PNG))).willReturn(prediction);

        assertThatThrownBy(() -> recognitionService.recognize(photo()))
                .isInstanceOfSatisfying(CustomException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.RECOGNITION_UNAVAILABLE));

        verifyNoInteractions(recognitionMatcher);
    }

    static Stream<RecognitionPrediction> invalidPredictions() {
        return Stream.of(null,
                new RecognitionPrediction(null, "plastic", 0.9),
                new RecognitionPrediction("", "plastic", 0.9),
                new RecognitionPrediction("test", null, 0.9),
                new RecognitionPrediction("test", " ", 0.9),
                new RecognitionPrediction("test", "plastic", null),
                new RecognitionPrediction("test", "plastic", -0.1),
                new RecognitionPrediction("test", "plastic", 1.1),
                new RecognitionPrediction("test", "plastic", Double.NaN),
                new RecognitionPrediction("test", "plastic", Double.POSITIVE_INFINITY));
    }

    private MockMultipartFile photo() throws Exception {
        return new MockMultipartFile("file", "photo.png", "image/png", RecognitionTestImages.image("png"));
    }
}
