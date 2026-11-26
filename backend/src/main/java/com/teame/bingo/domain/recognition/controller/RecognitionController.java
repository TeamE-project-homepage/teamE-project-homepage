package com.teame.bingo.domain.recognition.controller;

import com.teame.bingo.domain.recognition.dto.RecognitionMatchResult;
import com.teame.bingo.domain.recognition.service.RecognitionService;
import com.teame.bingo.global.exception.CustomException;
import com.teame.bingo.global.exception.ErrorCode;
import com.teame.bingo.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@Tag(name = "Recognition")
@RestController
@RequestMapping("/api/recognitions")
@RequiredArgsConstructor
public class RecognitionController {

    private final RecognitionService recognitionService;

    @Operation(
            summary = "Recognize a photo and return matching item candidates",
            description = "Public endpoint. Upload one JPEG/PNG image up to 10 MiB. Photos are not stored. "
                    + "Uses RecognitionMatcher with app.recognition.min-confidence (default 0.5). "
                    + "Low-confidence or unlinked results return 200 with matched=false and candidates=[].",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200", description = "RecognitionMatchResult, including unmatched results"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400", description = "Missing/empty file (RECOGNITION_400) or multiple files (COMMON_400)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "413", description = "Image exceeds 10 MiB or multipart request exceeds 11 MiB (COMMON_413)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "415", description = "Unsupported content type or image format (COMMON_415)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "422", description = "Unreadable image (RECOGNITION_422)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "503", description = "Recognition unavailable, timed out, or returned invalid data (RECOGNITION_503)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500", description = "An unexpected error occurred (COMMON_500)")
            })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<RecognitionMatchResult> recognize(
            @Parameter(description = "One JPEG/PNG photo, maximum 10 MiB", required = true)
            @RequestPart("file") MultipartFile file,
            @Parameter(hidden = true) MultipartHttpServletRequest request) {
        long fileCount = request.getMultiFileMap().values().stream().mapToLong(files -> files.size()).sum();
        if (fileCount > 1) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "file: upload exactly one photo");
        }
        return ApiResponse.ok(recognitionService.recognize(file));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingPhoto(MissingServletRequestPartException exception) {
        return ResponseEntity.status(ErrorCode.RECOGNITION_PHOTO_REQUIRED.getStatus())
                .body(ApiResponse.error(ErrorCode.RECOGNITION_PHOTO_REQUIRED));
    }
}
