package com.coding.cz.recon.service;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-20
 */


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class FlinkGatewayClient {

    private final WebClient webClient;

    public FlinkGatewayClient(@Value("${flink.gateway.url}") String flinkGatewayUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(flinkGatewayUrl)
                .build();
    }

    public Mono<String> executeSql(String sessionId, String sql) {
        Map<String, Object> body = Map.of(
                "statement", sql,
                "executionConfig", Map.of("parallelism", 4)
        );

        return webClient.post()
                .uri("/v1/sessions/{sessionId}/statements", sessionId)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(resp -> System.out.println("Flink Gateway Response: " + resp))
                .onErrorResume(e -> {
                    System.out.println("******出错*****" );
                    e.printStackTrace();
                    return Mono.just("FAILED: " + e.getMessage());
                });
    }
}
