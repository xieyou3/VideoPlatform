package com.videoplatform.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessExpireSeconds;
    private final long refreshExpireSeconds;

    public JwtTokenProvider(
            @Value("${security.jwt.secret:video-platform-demo-secret-video-platform-demo-secret}") String secret,
            @Value("${security.jwt.access-expire-seconds:1800}") long accessExpireSeconds,
            @Value("${security.jwt.refresh-expire-seconds:604800}") long refreshExpireSeconds) {
        byte[] keyBytes = secret.length() >= 32
                ? secret.getBytes(StandardCharsets.UTF_8)
                : Decoders.BASE64.decode("dmlkZW8tcGxhdGZvcm0tZGVtby1zZWNyZXQtdmlkZW8tcGxhdGZvcm0=");
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessExpireSeconds = accessExpireSeconds;
        this.refreshExpireSeconds = refreshExpireSeconds;
    }

    public String generateAccessToken(JwtUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claim("username", user.getUsername())
                .claim("roles", user.getRoles())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessExpireSeconds)))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(JwtUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claim("tokenType", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshExpireSeconds)))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public JwtUser parseUser(String token) {
        Claims claims = parseClaims(token);
        Object rolesObj = claims.get("roles");
        List<String> roles = rolesObj instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : List.of("ROLE_USER");
        return JwtUser.builder()
                .userId(Long.valueOf(claims.getSubject()))
                .username(String.valueOf(claims.get("username")))
                .roles(roles)
                .build();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return Long.valueOf(claims.getSubject());
    }

    public long getRefreshExpireSeconds() {
        return refreshExpireSeconds;
    }

    public long getAccessExpireSeconds() {
        return accessExpireSeconds;
    }

    public Key getSecretKey() {
        return secretKey;
    }
}
