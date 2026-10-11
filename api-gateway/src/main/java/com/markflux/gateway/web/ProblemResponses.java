package com.markflux.gateway.web;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class ProblemResponses {

    private final JsonMapper jsonMapper;

    public Map<String, Object> body(ServerWebExchange exchange, HttpStatus status, String errorCode, String detail) {
        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("title", status.getReasonPhrase());
        problem.put("status", status.value());
        problem.put("detail", detail);
        problem.put("instance", exchange.getRequest().getPath().value());
        problem.put("errorCode", errorCode);
        problem.put("timestamp", Instant.now().toString());
        String traceId = TraceIdFilter.traceIdOf(exchange);
        if (traceId != null) {
            problem.put("traceId", traceId);
        }
        return problem;
    }

    public Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String errorCode, String detail) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        byte[] json = jsonMapper.writeValueAsString(body(exchange, status, errorCode, detail))
                .getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(json)));
    }
}
