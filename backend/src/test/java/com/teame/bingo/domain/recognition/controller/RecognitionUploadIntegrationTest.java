package com.teame.bingo.domain.recognition.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Arrays;

import jakarta.servlet.MultipartConfigElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.teame.bingo.domain.recognition.RecognitionTestImages;
import com.teame.bingo.domain.recognition.client.MockRecognitionClient;
import com.teame.bingo.domain.recognition.client.RecognitionClient;
import com.teame.bingo.domain.recognition.dto.RecognitionMatchResult;
import com.teame.bingo.domain.recognition.service.RecognitionImageValidator;
import com.teame.bingo.domain.recognition.service.RecognitionMatcher;
import com.teame.bingo.domain.recognition.service.RecognitionService;
import com.teame.bingo.global.config.SecurityConfig;
import com.teame.bingo.global.exception.GlobalExceptionHandler;
import com.teame.bingo.global.exception.SecurityExceptionHandler;

@SpringBootTest(classes = RecognitionUploadIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.profiles.active=test",
            "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://test-project.supabase.co/auth/v1"
        })
class RecognitionUploadIntegrationTest {

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class
    })
    @Import({RecognitionController.class, RecognitionService.class, RecognitionImageValidator.class,
            GlobalExceptionHandler.class, SecurityConfig.class, SecurityExceptionHandler.class})
    static class TestApplication {

        @Bean
        RecognitionClient recognitionClient() {
            return new MockRecognitionClient();
        }
    }

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private MultipartConfigElement multipartConfig;

    @MockitoBean
    private RecognitionMatcher recognitionMatcher;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void stubMatcher() {
        given(recognitionMatcher.match("plastic", 0.91))
                .willReturn(RecognitionMatchResult.unmatched("plastic", 0.91));
    }

    @Test
    void actualMultipartParserAcceptsExactlyTenMiB() throws Exception {
        byte[] image = Arrays.copyOf(RecognitionTestImages.image("png"), RecognitionImageValidator.MAX_IMAGE_BYTES);

        var response = rest.postForEntity("/api/recognitions", upload(image), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("success").asBoolean()).isTrue();
        assertThat(response.getBody().path("data").path("label").asText()).isEqualTo("plastic");
        assertThat(response.getBody().path("data").path("candidates").isEmpty()).isTrue();
    }

    @Test
    void actualMultipartParserRejectsOversizedFileWithCommon413() {
        var response = rest.postForEntity("/api/recognitions",
                upload(new byte[RecognitionImageValidator.MAX_IMAGE_BYTES + 1]), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody().path("error").path("code").asText()).isEqualTo("COMMON_413");
    }

    @Test
    void malformedMultipartReturns400RatherThan500() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        var response = rest.postForEntity("/api/recognitions", new HttpEntity<>("missing boundary", headers), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().path("error").path("code").asText()).isEqualTo("COMMON_400");
    }

    @Test
    void acceptedPhotosStayBelowTheDiskSpoolingThreshold() {
        assertThat(multipartConfig.getMaxFileSize()).isEqualTo(10L * 1024 * 1024);
        assertThat(multipartConfig.getMaxRequestSize()).isEqualTo(11L * 1024 * 1024);
        assertThat(multipartConfig.getFileSizeThreshold()).isGreaterThanOrEqualTo((int) multipartConfig.getMaxFileSize());
    }

    @Test
    void swaggerDescribesMultipartUploadAndRecognitionResult() {
        var response = rest.getForEntity("/v3/api-docs", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode operation = response.getBody().path("paths").path("/api/recognitions").path("post");
        JsonNode schema = operation.path("requestBody").path("content").path("multipart/form-data").path("schema");
        if (schema.has("$ref")) {
            schema = response.getBody().at(schema.path("$ref").asText().substring(1));
        }
        assertThat(schema.path("properties").path("file").path("format").asText()).isEqualTo("binary");
        assertThat(schema.path("required").toString()).contains("\"file\"");
        assertThat(operation.path("responses").has("422")).isTrue();
        assertThat(operation.path("responses").has("503")).isTrue();
    }

    private HttpEntity<LinkedMultiValueMap<String, Object>> upload(byte[] image) {
        ByteArrayResource resource = new ByteArrayResource(image) {
            @Override
            public String getFilename() {
                return "photo.png";
            }
        };
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.IMAGE_PNG);
        LinkedMultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new HttpEntity<>(resource, fileHeaders));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return new HttpEntity<>(parts, headers);
    }
}
