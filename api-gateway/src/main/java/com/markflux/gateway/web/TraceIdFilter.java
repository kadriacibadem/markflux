package com.markflux.gateway.web;

import io.micrometer.tracing.handler.TracingObservationHandler;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class TraceIdFilter implements WebFilter, Ordered {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String traceId = traceIdOf(exchange);
        if (traceId != null) {
            exchange.getResponse().beforeCommit(() -> {
                exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId);
                return Mono.empty();
            });
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    // read traceID from Spring Tracing
    public static String traceIdOf(ServerWebExchange exchange) {
        return ServerRequestObservationContext.findCurrent(exchange.getAttributes())
                .map(context -> context.<TracingObservationHandler.TracingContext>get(
                        TracingObservationHandler.TracingContext.class))
                .map(tracing -> tracing.getSpan().context().traceId())
                .orElse(null);
    }
}
