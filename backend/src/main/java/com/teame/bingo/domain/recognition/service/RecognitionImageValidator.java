package com.teame.bingo.domain.recognition.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

import com.teame.bingo.global.exception.CustomException;
import com.teame.bingo.global.exception.ErrorCode;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class RecognitionImageValidator {

    public static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;

    public ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.RECOGNITION_PHOTO_REQUIRED);
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new CustomException(ErrorCode.PAYLOAD_TOO_LARGE);
        }
        MediaType contentType = imageContentType(file.getContentType());
        byte[] bytes;
        try (InputStream input = file.getInputStream()) {
            bytes = input.readNBytes(MAX_IMAGE_BYTES + 1);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        if (bytes.length == 0) {
            throw new CustomException(ErrorCode.RECOGNITION_PHOTO_REQUIRED);
        }
        if (bytes.length > MAX_IMAGE_BYTES) {
            throw new CustomException(ErrorCode.PAYLOAD_TOO_LARGE);
        }
        validateImageHeader(bytes, contentType);
        return new ValidatedImage(bytes, contentType);
    }

    private MediaType imageContentType(String value) {
        try {
            if (value != null) {
                MediaType type = MediaType.parseMediaType(value);
                if (MediaType.IMAGE_JPEG.includes(type)) {
                    return MediaType.IMAGE_JPEG;
                }
                if (MediaType.IMAGE_PNG.includes(type)) {
                    return MediaType.IMAGE_PNG;
                }
            }
        } catch (InvalidMediaTypeException e) {
            throw new CustomException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }
        throw new CustomException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    private void validateImageHeader(byte[] bytes, MediaType contentType) {
        // Avoid ImageIO's default disk cache and do not allocate a full decoded pixel buffer.
        try (MemoryCacheImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new CustomException(ErrorCode.RECOGNITION_UNREADABLE_IMAGE);
            }
            ImageReader reader = readers.next();
            try {
                String expected = MediaType.IMAGE_PNG.equals(contentType) ? "png" : "jpeg";
                if (!expected.equalsIgnoreCase(reader.getFormatName())) {
                    throw new CustomException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
                }
                reader.setInput(input, true, true);
                if (reader.getWidth(0) <= 0 || reader.getHeight(0) <= 0) {
                    throw new CustomException(ErrorCode.RECOGNITION_UNREADABLE_IMAGE);
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException | IndexOutOfBoundsException e) {
            throw new CustomException(ErrorCode.RECOGNITION_UNREADABLE_IMAGE);
        }
    }

    public record ValidatedImage(byte[] bytes, MediaType contentType) {
    }
}
