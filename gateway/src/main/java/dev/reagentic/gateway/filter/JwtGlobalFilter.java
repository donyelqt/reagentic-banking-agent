package dev.reagentic.gateway.filter;

import dev.reagentic.common.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class JwtGlobalFilter implements GlobalFilter, Ordered {

    @Value("${JWT_SECRET}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Defense in depth: internal service-to-service headers are never
        // accepted from external clients, regardless of the route.
        ServerHttpRequest cleaned = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove("X-Service-Token");
                    h.remove("X-User-Subject");
                })
                .build();
        ServerWebExchange exchangeWithCleanedHeaders = exchange.mutate().request(cleaned).build();

        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/api/auth/") || path.startsWith("/actuator/")) {
            return chain.filter(exchangeWithCleanedHeaders);
        }
        String token = JwtUtil.bearer(exchangeWithCleanedHeaders.getRequest().getHeaders().getFirst("Authorization"));
        if (token == null || token.isBlank()) {
            return unauthorized(exchangeWithCleanedHeaders, "MISSING_TOKEN", "Authorization header required");
        }
        try {
            JwtUtil.verify(jwtSecret, token);
            return chain.filter(exchangeWithCleanedHeaders);
        } catch (Exception e) {
            return unauthorized(exchangeWithCleanedHeaders, "INVALID_TOKEN", "Token verification failed");
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String code, String message) {
        return writeJson(exchange, HttpStatus.UNAUTHORIZED, code, message);
    }

    private Mono<Void> writeJson(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String json = "{\"status\":" + status.value() + ",\"code\":\"" + code + "\",\"message\":\"" + message + "\"}";
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
