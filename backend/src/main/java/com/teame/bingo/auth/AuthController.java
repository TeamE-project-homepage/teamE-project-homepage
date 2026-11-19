package com.teame.bingo.auth;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.teame.bingo.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Operation(summary = "Get the authenticated Supabase user")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(new CurrentUserResponse(
                UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("email")));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CurrentUserResponse(UUID id, String email) {
    }
}
