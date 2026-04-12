package com.videoplatform.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videoplatform.auth.domain.User;
import com.videoplatform.auth.dto.AuthResponse;
import com.videoplatform.auth.dto.LoginRequest;
import com.videoplatform.auth.dto.RegisterRequest;
import com.videoplatform.auth.mapper.UserMapper;
import com.videoplatform.common.security.JwtTokenProvider;
import com.videoplatform.common.security.JwtUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
        User existed = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, request.getEmail()));
        if (existed != null) {
            throw new IllegalArgumentException("邮箱已注册");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus("ACTIVE");
        user.setFansCount(0L);
        user.setFollowingCount(0L);
        user.setLikedCount(0L);
        userMapper.insert(user);
        return buildTokenResponse(user, response);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, request.getEmail()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("邮箱或密码错误");
        }
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
        return buildTokenResponse(user, response);
    }

    public AuthResponse refresh(String refreshToken, HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("refresh token 不存在");
        }
        
        try {
            JwtUser jwtUser = jwtTokenProvider.parseUser(refreshToken);
            User user = userMapper.selectById(jwtUser.getUserId());
            if (user == null) {
                throw new IllegalArgumentException("用户不存在");
            }
            return buildTokenResponse(user, response);
        } catch (Exception e) {
            throw new IllegalArgumentException("refresh token 已失效: " + e.getMessage());
        }
    }

    public void logout(String refreshToken, HttpServletResponse response) {
        Cookie accessCookie = new Cookie("access_token", "");
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refresh_token", "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);
    }

    public AuthResponse.UserInfo getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return mapUser(user);
    }

    private AuthResponse buildTokenResponse(User user, HttpServletResponse response) {
        JwtUser jwtUser = JwtUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .roles(List.of("ROLE_USER"))
                .build();
        String accessToken = jwtTokenProvider.generateAccessToken(jwtUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken(jwtUser);

        Cookie accessCookie = new Cookie("access_token", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false);
        accessCookie.setPath("/");
        accessCookie.setMaxAge((int) jwtTokenProvider.getAccessExpireSeconds());
        accessCookie.setAttribute("SameSite", "Strict");
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge((int) jwtTokenProvider.getRefreshExpireSeconds());
        refreshCookie.setAttribute("SameSite", "Strict");
        response.addCookie(refreshCookie);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(null)
                .user(mapUser(user))
                .build();
    }

    private AuthResponse.UserInfo mapUser(User user) {
        return AuthResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatar(user.getAvatarUrl())
                .roles(List.of("ROLE_USER"))
                .build();
    }
}
