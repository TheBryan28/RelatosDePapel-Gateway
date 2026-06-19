package com.relatosdepapel.Gateway.filter;

import com.relatosdepapel.Gateway.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final AuthService authService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        log.debug("Requested access to URI: {} {} ", request.getMethod().name(), path);

        if (isPublicEndpoint(path, request) || request.getHeaders().getFirst("Stripe-Signature") != null) {
            log.debug("Access granted - public endpoint: {}", path);
            return chain.filter(exchange);
        }

        String sessionId = extractToken(request);
        if (sessionId == null || sessionId.isEmpty()) {
            log.warn("Access Denied - missing or invalid Authorization header or sessionId Cookie");
            return respondWithError(exchange, HttpStatus.FORBIDDEN, "Authorization header or sessionId Cookie missing or invalid ");
        }

        // Validar token de forma simple - solo 200 o 401
        return authService.validateToken(sessionId)
                .flatMap(response -> {
                    log.debug("Valid session received for sessionId: {}", sessionId);
                    // Token válido - agregar access token y continuar
                    ServerHttpRequest modifiedRequest = request.mutate()
                            .header("accessToken", response.getAccessToken())
                            .build();
                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                })
                .onErrorResume(error -> {
                    // Token inválido o expirado - devolver 401
                    log.warn("Invalid or expired token for sessionId: {}", sessionId);
                    return respondWithError(exchange, HttpStatus.UNAUTHORIZED, "Token invalid or expired");
                });
    }

    private String extractToken(ServerHttpRequest request) {
        // primero intenta cookie
        List<HttpCookie> cookies = request.getCookies().get("sessionId");
        if (cookies != null && !cookies.isEmpty()) {
            return cookies.getFirst().getValue();
        }

        // fallback a Bearer header (útil para Postman/mobile)
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        return null;
    }

    private boolean isPublicEndpoint(String path, ServerHttpRequest request) {
        // Permitir solo POST a tokens (para login) - GET no debe permitirse
        // permitir post para users (registro)
        return (path.matches(".*/(users/)?api/v1/token/?$") && "POST".equals(request.getMethod().name()))
                || (path.matches(".*/(users/)?api/v1/users*") && "POST".equals(request.getMethod().name()))
                || path.matches(".*/(catalog/)?api/.*")
                || path.matches(".*(communications/)?ws-api/.*");
    }

    private Mono<Void> respondWithError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");

        String body = String.format("{\"error\": \"%s\", \"message\": \"%s\"}",
                status.getReasonPhrase(), message);

        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return -100; // Ejecutar antes que otros filtros
    }
}
