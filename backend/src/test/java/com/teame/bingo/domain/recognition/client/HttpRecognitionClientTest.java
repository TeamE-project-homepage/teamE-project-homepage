package com.teame.bingo.domain.recognition.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teame.bingo.domain.recognition.RecognitionTestImages;
import com.teame.bingo.domain.recognition.dto.RecognitionPrediction;
import com.teame.bingo.global.exception.CustomException;
import com.teame.bingo.global.exception.ErrorCode;

class HttpRecognitionClientTest {

    private MockRestServiceServer server;
    private HttpRecognitionClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://recognition.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpRecognitionClient(builder.build(), new ObjectMapper());
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void forwardsFileBytesMimeTypeAndMultipartFieldName() throws Exception {
        byte[] image = RecognitionTestImages.image("png");
        server.expect(requestTo("http://recognition.test/predict"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    assertThat(request.getHeaders().getContentType().isCompatibleWith(MediaType.MULTIPART_FORM_DATA)).isTrue();
                    String multipart = ((MockClientHttpRequest) request).getBodyAsString(StandardCharsets.ISO_8859_1);
                    assertThat(multipart).contains("name=\"file\"", "filename=\"upload.png\"", "Content-Type: image/png");
                    assertThat(multipart).contains(new String(image, StandardCharsets.ISO_8859_1));
                })
                .andRespond(withSuccess("{\"model\":\"test\",\"label\":\"plastic\",\"score\":0.8123}", MediaType.APPLICATION_JSON));

        assertThat(client.predict(image, MediaType.IMAGE_PNG))
                .isEqualTo(new RecognitionPrediction("test", "plastic", 0.8123));
    }

    @Test
    void mapsFastApiDecode400ToPublic422() {
        server.expect(requestTo("http://recognition.test/predict"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":\"Could not read the image.\"}"));

        assertError(ErrorCode.RECOGNITION_UNREADABLE_IMAGE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"detail\":\"File must be an image.\"}", "{\"detail\":[]}", "not json", ""})
    void unrelatedOrMalformed400ResponseIsUnavailable(String errorBody) {
        server.expect(requestTo("http://recognition.test/predict"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(errorBody));

        assertError(ErrorCode.RECOGNITION_UNAVAILABLE);
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 422, 429, 500, 502, 503, 504})
    void upstreamFailuresBecome503(int status) {
        server.expect(requestTo("http://recognition.test/predict"))
                .andRespond(withStatus(HttpStatus.valueOf(status)));

        assertError(ErrorCode.RECOGNITION_UNAVAILABLE);
    }

    @Test
    void mapsUpstreamPayloadLimitTo413() {
        server.expect(requestTo("http://recognition.test/predict"))
                .andRespond(withStatus(HttpStatus.PAYLOAD_TOO_LARGE));

        assertError(ErrorCode.PAYLOAD_TOO_LARGE);
    }

    @Test
    void timeoutBecomes503() {
        server.expect(requestTo("http://recognition.test/predict"))
                .andRespond(request -> { throw new SocketTimeoutException("Read timed out"); });

        assertError(ErrorCode.RECOGNITION_UNAVAILABLE);
    }

    @Test
    void connectionFailureBecomes503() {
        server.expect(requestTo("http://recognition.test/predict"))
                .andRespond(request -> { throw new ConnectException("Connection refused"); });

        assertError(ErrorCode.RECOGNITION_UNAVAILABLE);
    }

    @Test
    void malformedSuccessJsonBecomes503() {
        server.expect(requestTo("http://recognition.test/predict"))
                .andRespond(withSuccess("not json", MediaType.APPLICATION_JSON));

        assertError(ErrorCode.RECOGNITION_UNAVAILABLE);
    }

    private void assertError(ErrorCode code) {
        assertThatThrownBy(() -> client.predict(new byte[]{1, 2, 3}, MediaType.IMAGE_JPEG))
                .isInstanceOfSatisfying(CustomException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(code));
    }
}
