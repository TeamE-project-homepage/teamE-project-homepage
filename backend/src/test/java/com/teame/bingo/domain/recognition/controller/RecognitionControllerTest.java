package com.teame.bingo.domain.recognition.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.teame.bingo.domain.item.dto.ItemSummaryResponse;
import com.teame.bingo.domain.recognition.RecognitionTestImages;
import com.teame.bingo.domain.recognition.client.RecognitionClient;
import com.teame.bingo.domain.recognition.dto.RecognitionMatchResult;
import com.teame.bingo.domain.recognition.dto.RecognitionPrediction;
import com.teame.bingo.domain.recognition.service.RecognitionImageValidator;
import com.teame.bingo.domain.recognition.service.RecognitionMatcher;
import com.teame.bingo.domain.recognition.service.RecognitionService;
import com.teame.bingo.global.config.SecurityConfig;
import com.teame.bingo.global.exception.CustomException;
import com.teame.bingo.global.exception.ErrorCode;
import com.teame.bingo.global.exception.SecurityExceptionHandler;

@WebMvcTest(RecognitionController.class)
@Import({SecurityConfig.class, SecurityExceptionHandler.class, RecognitionService.class, RecognitionImageValidator.class})
class RecognitionControllerTest {

    private static final String PATH = "/api/recognitions";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecognitionClient recognitionClient;

    @MockitoBean
    private RecognitionMatcher recognitionMatcher;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void publiclyRecognizesPngAndPreservesAllCandidateFields() throws Exception {
        byte[] image = RecognitionTestImages.image("png");
        given(recognitionClient.predict(any(byte[].class), eq(MediaType.IMAGE_PNG)))
                .willReturn(new RecognitionPrediction("test", "plastic", 0.8123));
        given(recognitionMatcher.match("plastic", 0.8123)).willReturn(RecognitionMatchResult.matched(
                "plastic", 0.8123, List.of(
                        new ItemSummaryResponse(3L, "Clear PET Bottle", 1L, "PL", "Plastic", 2L),
                        new ItemSummaryResponse(4L, "Plastic Container", 1L, "PL", "Plastic", 2L))));

        mockMvc.perform(multipart(PATH).file(new MockMultipartFile("file", "photo.png", "image/png", image)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.matched").value(true))
                .andExpect(jsonPath("$.data.label").value("plastic"))
                .andExpect(jsonPath("$.data.score").value(0.8123))
                .andExpect(jsonPath("$.data.candidates.length()").value(2))
                .andExpect(jsonPath("$.data.candidates[0].itemId").value(3))
                .andExpect(jsonPath("$.data.candidates[0].name").value("Clear PET Bottle"))
                .andExpect(jsonPath("$.data.candidates[0].categoryId").value(1))
                .andExpect(jsonPath("$.data.candidates[0].categoryCode").value("PL"))
                .andExpect(jsonPath("$.data.candidates[0].categoryName").value("Plastic"))
                .andExpect(jsonPath("$.data.candidates[0].disposalTypeId").value(2))
                .andExpect(jsonPath("$.data.model").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(recognitionClient).predict(eq(image), eq(MediaType.IMAGE_PNG));
    }

    @Test
    void jpegLowConfidenceReturnsSuccessfulEmptyCandidates() throws Exception {
        given(recognitionClient.predict(any(byte[].class), eq(MediaType.IMAGE_JPEG)))
                .willReturn(new RecognitionPrediction("test", "plastic", 0.49));
        given(recognitionMatcher.match("plastic", 0.49)).willReturn(RecognitionMatchResult.unmatched("plastic", 0.49));

        mockMvc.perform(multipart(PATH).file(new MockMultipartFile(
                        "file", "photo.jpg", "image/jpeg", RecognitionTestImages.image("jpeg"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matched").value(false))
                .andExpect(jsonPath("$.data.candidates").isEmpty());
    }

    @Test
    void rejectsMissingFileWithDomainError() throws Exception {
        mockMvc.perform(multipart(PATH))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RECOGNITION_400"))
                .andExpect(jsonPath("$.error.message").value("Please select a photo to upload."));

        verifyNoInteractions(recognitionClient, recognitionMatcher);
    }

    @Test
    void rejectsEmptyFile() throws Exception {
        mockMvc.perform(multipart(PATH).file(new MockMultipartFile("file", "empty.png", "image/png", new byte[0])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("RECOGNITION_400"));

        verifyNoInteractions(recognitionClient, recognitionMatcher);
    }

    @Test
    void rejectsMultipleFiles() throws Exception {
        byte[] image = RecognitionTestImages.image("png");
        mockMvc.perform(multipart(PATH)
                        .file(new MockMultipartFile("file", "a.png", "image/png", image))
                        .file(new MockMultipartFile("file", "b.png", "image/png", image)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_400"));

        verifyNoInteractions(recognitionClient, recognitionMatcher);
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/gif", "image/webp", "image/*", "text/plain", "invalid"})
    void rejectsUnsupportedPartContentType(String contentType) throws Exception {
        mockMvc.perform(multipart(PATH).file(new MockMultipartFile(
                        "file", "photo.png", contentType, RecognitionTestImages.image("png"))))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.code").value("COMMON_415"));

        verifyNoInteractions(recognitionClient, recognitionMatcher);
    }

    @Test
    void rejectsNonMultipartContentType() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.code").value("COMMON_415"));
    }

    @Test
    void rejectsCorruptedImage() throws Exception {
        mockMvc.perform(multipart(PATH).file(new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3})))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("RECOGNITION_422"));

        verifyNoInteractions(recognitionClient, recognitionMatcher);
    }

    @Test
    void exposesUnavailableServiceAs503() throws Exception {
        given(recognitionClient.predict(any(byte[].class), eq(MediaType.IMAGE_PNG)))
                .willThrow(new CustomException(ErrorCode.RECOGNITION_UNAVAILABLE));

        mockMvc.perform(multipart(PATH).file(new MockMultipartFile(
                        "file", "photo.png", "image/png", RecognitionTestImages.image("png"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("RECOGNITION_503"))
                .andExpect(jsonPath("$.error.message").value(
                        "Photo recognition is temporarily unavailable. Please search by name instead."));

        verifyNoInteractions(recognitionMatcher);
    }
}
