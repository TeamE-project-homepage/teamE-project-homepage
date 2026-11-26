package com.teame.bingo.domain.recognition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.teame.bingo.domain.recognition.RecognitionTestImages;
import com.teame.bingo.global.exception.CustomException;
import com.teame.bingo.global.exception.ErrorCode;

class RecognitionImageValidatorTest {

    private final RecognitionImageValidator validator = new RecognitionImageValidator();

    @Test
    void acceptsExactlyTenMiBAndReturnsOriginalBytes() throws Exception {
        byte[] image = Arrays.copyOf(RecognitionTestImages.image("png"), RecognitionImageValidator.MAX_IMAGE_BYTES);

        var result = validator.validate(new MockMultipartFile("file", "photo.png", "image/png", image));

        assertThat(result.bytes()).containsExactly(image);
        assertThat(result.contentType()).isEqualTo(MediaType.IMAGE_PNG);
    }

    @Test
    void rejectsOneByteAboveLimit() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png",
                new byte[RecognitionImageValidator.MAX_IMAGE_BYTES + 1]);

        assertError(file, ErrorCode.PAYLOAD_TOO_LARGE);
    }

    @Test
    void boundedReadRejectsOversizedStreamEvenWhenReportedSizeIsWrong() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(1L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[RecognitionImageValidator.MAX_IMAGE_BYTES + 1]));

        assertError(file, ErrorCode.PAYLOAD_TOO_LARGE);
    }

    @Test
    void rejectsGifDisguisedAsJpeg() throws Exception {
        assertError(new MockMultipartFile("file", "fake.jpg", "image/jpeg", RecognitionTestImages.image("gif")),
                ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void rejectsPngDeclaredAsJpeg() throws Exception {
        assertError(new MockMultipartFile("file", "fake.jpg", "image/jpeg", RecognitionTestImages.image("png")),
                ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void rejectsTruncatedPngHeader() {
        assertError(new MockMultipartFile("file", "photo.png", "image/png",
                new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10}), ErrorCode.RECOGNITION_UNREADABLE_IMAGE);
    }

    @Test
    void rejectsMissingContentType() throws Exception {
        assertError(new MockMultipartFile("file", "photo.png", null, RecognitionTestImages.image("png")),
                ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void inputReadFailureReturnsServerError() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(1L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getInputStream()).thenThrow(new IOException("Failed to read upload"));

        assertError(file, ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private void assertError(MultipartFile file, ErrorCode errorCode) {
        assertThatThrownBy(() -> validator.validate(file)).isInstanceOfSatisfying(CustomException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(errorCode));
    }
}
