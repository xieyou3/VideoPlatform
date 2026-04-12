package com.videoplatform.auth.controller;

import com.videoplatform.auth.dto.AuthResponse;
import com.videoplatform.auth.dto.LoginRequest;
import com.videoplatform.auth.dto.RefreshTokenRequest;
import com.videoplatform.auth.dto.RegisterRequest;
import com.videoplatform.auth.service.AuthService;
import com.videoplatform.common.api.ApiResponse;
import com.videoplatform.common.security.JwtTokenProvider;
import com.videoplatform.common.security.JwtUser;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                              HttpServletResponse response) {
        return ApiResponse.success(authService.register(request, response));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                           HttpServletResponse response) {
        return ApiResponse.success(authService.login(request, response));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(
            @CookieValue(value = "refresh_token", required = false) String cookieRefreshToken,
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletResponse response) {
        String token = request != null && request.getRefreshToken() != null
                ? request.getRefreshToken()
                : cookieRefreshToken;
        return ApiResponse.success(authService.refresh(token, response));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CookieValue(value = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken, response);
        return ApiResponse.success("OK", null);
    }

    @GetMapping("/me")
    public ApiResponse<AuthResponse.UserInfo> me(
            @CookieValue(value = "access_token", required = false) String cookieAccessToken,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = null;
        
        if (cookieAccessToken != null && !cookieAccessToken.isBlank()) {
            token = cookieAccessToken;
        } else if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }
        
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("未提供有效的认证令牌");
        }
        
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        return ApiResponse.success(authService.getCurrentUser(userId));
    }
}
