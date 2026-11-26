package com.teame.bingo.domain.recognition.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.teame.bingo.domain.recognition.client.HttpRecognitionClient;
import com.teame.bingo.domain.recognition.client.MockRecognitionClient;
import com.teame.bingo.domain.recognition.client.RecognitionClient;
import com.teame.bingo.global.exception.CustomException;
import com.teame.bingo.global.exception.ErrorCode;

class RecognitionClientConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(context -> context.getBeanFactory().setConversionService(ApplicationConversionService.getSharedInstance()))
            .withUserConfiguration(RecognitionClientConfig.class)
            .withBean(ObjectMapper.class, () -> new ObjectMapper());

    @Test
    void defaultsToHttpWithoutMakingARequestAtStartup() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(RecognitionClient.class);
            assertThat(context.getBean(RecognitionClient.class)).isInstanceOf(HttpRecognitionClient.class);
        });
    }

    @Test
    void selectsMockOnlyWhenExplicitlyConfigured() {
        runner.withPropertyValues("app.recognition.mode=mock").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(RecognitionClient.class);
            RecognitionClient client = context.getBean(RecognitionClient.class);
            assertThat(client).isInstanceOf(MockRecognitionClient.class);
            assertThat(client.predict(new byte[]{1}, MediaType.IMAGE_PNG).label()).isEqualTo("plastic");
        });
    }

    @Test
    void selectsHttpClientFromConfiguration() {
        runner.withPropertyValues("app.recognition.mode=http", "app.recognition.base-url=http://localhost:8000",
                        "app.recognition.connect-timeout=250ms", "app.recognition.read-timeout=5s")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(RecognitionClient.class);
                    assertThat(context.getBean(RecognitionClient.class)).isInstanceOf(HttpRecognitionClient.class);
                });
    }

    @Test
    void rejectsUnknownModeInsteadOfSilentlyUsingMock() {
        runner.withPropertyValues("app.recognition.mode=invalid")
                .run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0ms", "-1s", "0.000001ms", "2147483648ms"})
    void rejectsUnboundedOrInvalidTimeouts(String timeout) {
        runner.withPropertyValues("app.recognition.mode=http", "app.recognition.read-timeout=" + timeout)
                .run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://localhost", "http:///missing-host", "http://user:secret@localhost", "http://localhost?query=1"})
    void rejectsInvalidServiceUrl(String url) {
        runner.withPropertyValues("app.recognition.mode=http", "app.recognition.base-url=" + url)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void configuredReadTimeoutActuallyStopsWaitingForAiServer() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        CountDownLatch release = new CountDownLatch(1);
        server.createContext("/predict", exchange -> {
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            RecognitionClient client = new RecognitionClientConfig().recognitionClient(
                    "http", "http://127.0.0.1:" + server.getAddress().getPort(),
                    Duration.ofSeconds(1), Duration.ofMillis(100), new ObjectMapper());

            assertThatThrownBy(() -> client.predict(new byte[]{1}, MediaType.IMAGE_PNG))
                    .isInstanceOfSatisfying(CustomException.class,
                            error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.RECOGNITION_UNAVAILABLE));
        } finally {
            release.countDown();
            server.stop(0);
        }
    }
}
