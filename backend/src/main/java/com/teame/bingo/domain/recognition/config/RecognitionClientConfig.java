package com.teame.bingo.domain.recognition.config;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teame.bingo.domain.recognition.client.HttpRecognitionClient;
import com.teame.bingo.domain.recognition.client.MockRecognitionClient;
import com.teame.bingo.domain.recognition.client.RecognitionClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RecognitionClientConfig {

    @Bean
    public RecognitionClient recognitionClient(
            @Value("${app.recognition.mode:http}") String mode,
            @Value("${app.recognition.base-url:http://localhost:8000}") String baseUrl,
            @Value("${app.recognition.connect-timeout:2s}") Duration connectTimeout,
            @Value("${app.recognition.read-timeout:30s}") Duration readTimeout,
            ObjectMapper objectMapper) {
        if ("mock".equals(mode.toLowerCase(Locale.ROOT))) {
            return new MockRecognitionClient();
        }
        if (!"http".equals(mode.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("app.recognition.mode must be mock or http");
        }
        URI uri = URI.create(baseUrl);
        if ((!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("app.recognition.base-url must be an HTTP(S) service URL");
        }
        validateTimeout(connectTimeout);
        validateTimeout(readTimeout);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new HttpRecognitionClient(RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build(), objectMapper);
    }

    private void validateTimeout(Duration timeout) {
        if (timeout.isNegative() || timeout.isZero() || timeout.toMillis() < 1 || timeout.toMillis() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Recognition timeouts must be between 1ms and 2147483647ms");
        }
    }
}
