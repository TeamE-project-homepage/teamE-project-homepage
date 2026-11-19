package com.teame.bingo.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import com.teame.bingo.global.config.SecurityConfig;
import com.teame.bingo.global.config.SupabaseJwtConfig;
import com.teame.bingo.global.exception.SecurityExceptionHandler;
import com.teame.bingo.global.response.ApiResponse;
import com.teame.bingo.health.HealthController;

@WebMvcTest({AuthController.class, HealthController.class, AuthSecurityTest.PublicController.class})
@Import({SecurityConfig.class, SupabaseJwtConfig.class, SecurityExceptionHandler.class,
        AuthSecurityTest.PublicController.class})
class AuthSecurityTest {

    private static final String ISSUER = "https://test-project.supabase.co/auth/v1";
    private static final String USER_ID = UUID.randomUUID().toString();
    private static final ECKey KEY;
    private static final ECKey ROTATED_KEY;
    private static final RSAKey RSA_KEY;
    private static final HttpServer JWKS;

    static {
        try {
            KEY = new ECKeyGenerator(Curve.P_256).keyID("test-key").generate();
            ROTATED_KEY = new ECKeyGenerator(Curve.P_256).keyID("rotated-key").generate();
            RSA_KEY = new RSAKeyGenerator(2048).keyID("rsa-key").generate();
            JWKS = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            JWKS.createContext("/jwks", exchange -> {
                byte[] body = new JWKSet(List.of(KEY.toPublicJWK(), ROTATED_KEY.toPublicJWK(),
                        RSA_KEY.toPublicJWK())).toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (var stream = exchange.getResponseBody()) {
                    stream.write(body);
                }
            });
            JWKS.start();
        } catch (JOSEException | IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> ISSUER);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://127.0.0.1:" + JWKS.getAddress().getPort() + "/jwks");
    }

    @AfterAll
    static void stopJwks() {
        JWKS.stop(0);
    }

    @Test
    void guestCanUsePublicGetAndPostWithoutCsrf() throws Exception {
        mockMvc.perform(get("/health")).andExpect(status().isOk());
        mockMvc.perform(get("/api/test-public")).andExpect(status().isOk());
        mockMvc.perform(post("/api/test-public")).andExpect(status().isOk());
    }

    @Test
    void guestCannotAccessIdentityOrPersonalNamespace() throws Exception {
        for (String path : List.of("/api/auth/me", "/api/me", "/api/me/bookmarks")) {
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("COMMON_401"));
        }
    }

    @Test
    void verifiesEs256SignatureAndReturnsOnlyUserIdentity() throws Exception {
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(claims())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(USER_ID))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.access_token").doesNotExist())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void acceptsAnotherPublishedKey() throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256)
                .keyID(ROTATED_KEY.getKeyID()).build(), claims().build());
        jwt.sign(new ECDSASigner(ROTATED_KEY));
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.serialize()))
                .andExpect(status().isOk());
    }

    @Test
    void acceptsRs256Signature() throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(RSA_KEY.getKeyID()).build(), claims().build());
        jwt.sign(new RSASSASigner(RSA_KEY));
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.serialize()))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedUserCanStillUsePublicApi() throws Exception {
        mockMvc.perform(get("/api/test-public").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(claims())))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        assertRejected(token(claims().expirationTime(Date.from(Instant.now().minusSeconds(120)))));
    }

    @Test
    void rejectsWrongIssuer() throws Exception {
        assertRejected(token(claims().issuer("https://other-project.supabase.co/auth/v1")));
    }

    @Test
    void rejectsWrongAudience() throws Exception {
        assertRejected(token(claims().audience("anon")));
    }

    @Test
    void rejectsMissingAudience() throws Exception {
        assertRejected(token(claims().audience((List<String>) null)));
    }

    @Test
    void rejectsMissingExpiration() throws Exception {
        assertRejected(token(claims().expirationTime(null)));
    }

    @Test
    void rejectsMissingOrInvalidUserId() throws Exception {
        assertRejected(token(claims().subject(null)));
        assertRejected(token(claims().subject("not-a-user-id")));
    }

    @Test
    void rejectsTokenThatIsNotYetValid() throws Exception {
        assertRejected(token(claims().notBeforeTime(Date.from(Instant.now().plusSeconds(300)))));
    }

    @Test
    void rejectsForgedSignatureEvenWithCorrectKeyId() throws Exception {
        ECKey attackerKey = new ECKeyGenerator(Curve.P_256).generate();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256)
                .keyID(KEY.getKeyID()).build(), claims().build());
        jwt.sign(new ECDSASigner(attackerKey));
        assertRejected(jwt.serialize());
    }

    @Test
    void rejectsMalformedTokenAndPublishableKey() throws Exception {
        assertRejected("not-a-jwt");
        assertRejected("sb_publishable_this-is-not-a-login-token");
    }

    @Test
    void invalidTokenDoesNotBecomeAGuestOnPublicApi() throws Exception {
        mockMvc.perform(get("/api/test-public").header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("COMMON_401"));
    }

    @Test
    void acceptsPreflightFromConfiguredFrontendWithoutAuthentication() throws Exception {
        mockMvc.perform(options("/api/auth/me")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5500")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5500"))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
    }

    @Test
    void rejectsUnconfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/me")
                        .header(HttpHeaders.ORIGIN, "https://untrusted.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void unauthorizedResponseStillHasCorsHeaders() throws Exception {
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.ORIGIN, "http://localhost:5500"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5500"));
    }

    private void assertRejected(String token) throws Exception {
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("COMMON_401"));
    }

    private static JWTClaimsSet.Builder claims() {
        return new JWTClaimsSet.Builder().issuer(ISSUER).audience("authenticated")
                .subject(USER_ID).claim("email", "user@example.com")
                .issueTime(new Date()).expirationTime(Date.from(Instant.now().plusSeconds(300)));
    }

    private static String token(JWTClaimsSet.Builder claims) throws JOSEException {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256)
                .keyID(KEY.getKeyID()).build(), claims.build());
        jwt.sign(new ECDSASigner(KEY));
        return jwt.serialize();
    }

    @RestController
    static class PublicController {
        @GetMapping("/api/test-public")
        ApiResponse<Void> getPublic() {
            return ApiResponse.ok();
        }

        @PostMapping("/api/test-public")
        ApiResponse<Void> postPublic() {
            return ApiResponse.ok();
        }
    }
}
