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
        String authHeader = exchange.getRequest().getHeaders().getFirst(SecurityConstants.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return chain.filter(exchange);
        }
        String token = authHeader.substring(SecurityConstants.BEARER_PREFIX.length());
        JwtUser user = jwtTokenProvider.parseUser(token);
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(SecurityConstants.USER_ID_HEADER, String.valueOf(user.getUserId()))
                .header(SecurityConstants.USERNAME_HEADER, user.getUsername())
                .header(SecurityConstants.ROLE_HEADER, String.join(",", user.getRoles()))
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
