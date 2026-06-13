package com.relatosdepapel.Gateway.service;

import com.relatosdepapel.Gateway.model.SessionValidationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class AuthService {

    @LoadBalanced
    private final WebClient.Builder webClientBuilder;

    private final String userServiceUrl;

    public AuthService(WebClient.Builder webClientBuilder, @Value("${microservices.users.url}") String userServiceUrl) {
        this.webClientBuilder = webClientBuilder;
        this.userServiceUrl = userServiceUrl;
    }

    /**
     * Valida un token opaco llamando al microservicio de usuarios.
     * Solo puede devolver 200 (válido) o error (inválido/expirado).
     */
    public Mono<SessionValidationResponse> validateToken(String tokenId) {
        System.out.println("Validating token with ID: " + tokenId);
        return webClientBuilder.build()
                .get()
                .uri(userServiceUrl.concat("/token/{tokenId}"), tokenId)
                .retrieve()
                .bodyToMono(SessionValidationResponse.class)
                .doOnNext(response -> log.debug("Token validation successful for tokenId: {}", tokenId))
                .doOnError(error -> log.warn("Token validation failed for tokenId: {} - {}", tokenId, error.getMessage()));
    }
}
