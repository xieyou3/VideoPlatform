package com.videoplatform.gateway.filter;

import com.videoplatform.common.constant.SecurityConstants;
import com.videoplatform.common.security.JwtTokenProvider;
import com.videoplatform.common.security.JwtUser;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtRelayFilter implements GlobalFilter, Ordered {

    private final JwtTokenProvider jwtTokenProvider;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (pathMatcher.match("/api/auth/**", path)) {
            return chain.filter(exchange);
        }
        
        String token = extractToken(exchange);
        
        if (token == null || token.isBlank()) {
            return chain.filter(exchange);
        }
        
        try {
            JwtUser user = jwtTokenProvider.parseUser(token);
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .header(SecurityConstants.USER_ID_HEADER, String.valueOf(user.getUserId()))
                    .header(SecurityConstants.USERNAME_HEADER, user.getUsername())
                    .header(SecurityConstants.ROLE_HEADER, String.join(",", user.getRoles()))
                    .build();
            return chain.filter(exchange.mutate().request(request).build());
        } catch (Exception e) {
            return chain.filter(exchange);
        }
    }
    
    private String extractToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(SecurityConstants.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return authHeader.substring(SecurityConstants.BEARER_PREFIX.length());
        }
        
        if (exchange.getRequest().getCookies().containsKey(SecurityConstants.ACCESS_TOKEN_COOKIE)) {
            return exchange.getRequest().getCookies().getFirst(SecurityConstants.ACCESS_TOKEN_COOKIE).getValue();
        }
        
        return null;
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
