package com.teame.bingo.domain.recognition.client;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teame.bingo.domain.recognition.dto.RecognitionPrediction;
import com.teame.bingo.global.exception.CustomException;
import com.teame.bingo.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@RequiredArgsConstructor
public class HttpRecognitionClient implements RecognitionClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Override
    public RecognitionPrediction predict(byte[] image, MediaType contentType) {
        ByteArrayResource resource = new ByteArrayResource(image) {
            @Override
            public String getFilename() {
                return MediaType.IMAGE_PNG.includes(contentType) ? "upload.png" : "upload.jpg";
            }
        };
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(contentType);
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(resource, partHeaders));
        try {
            return restClient.post().uri("/predict")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (request, response) -> {
                        throw upstreamError(response);
                    })
                    .body(RecognitionPrediction.class);
        } catch (RestClientException e) {
            throw new CustomException(ErrorCode.RECOGNITION_UNAVAILABLE);
        }
    }

    private CustomException upstreamError(ClientHttpResponse response) throws IOException {
        if (response.getStatusCode().value() == 413) {
            return new CustomException(ErrorCode.PAYLOAD_TOO_LARGE);
        }
        if (response.getStatusCode().value() == 400) {
            try {
                JsonNode error = objectMapper.readTree(response.getBody());
                if (error != null && "Could not read the image.".equals(error.path("detail").asText())) {
                    return new CustomException(ErrorCode.RECOGNITION_UNREADABLE_IMAGE);
                }
            } catch (IOException e) {
                return new CustomException(ErrorCode.RECOGNITION_UNAVAILABLE);
            }
        }
        return new CustomException(ErrorCode.RECOGNITION_UNAVAILABLE);
    }
}
