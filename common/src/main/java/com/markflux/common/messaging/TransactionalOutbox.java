package com.markflux.common.messaging;

import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

@Component
public class TransactionalOutbox {

    private final JdbcClient jdbcClient;
    private final JsonMapper jsonMapper;
    private final Tracer tracer;
    private final Propagator propagator;

    public TransactionalOutbox(JdbcClient jdbcClient, JsonMapper jsonMapper, ObjectProvider<Tracer> tracer,
            ObjectProvider<Propagator> propagator) {
        this.jdbcClient = jdbcClient;
        this.jsonMapper = jsonMapper;
        this.tracer = tracer.getIfAvailable(() -> Tracer.NOOP);
        this.propagator = propagator.getIfAvailable(() -> Propagator.NOOP);
    }

    public void save(String topic, String key, String eventType, Object payload) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Outbox events must be saved inside a transaction: " + eventType);
        }
        jdbcClient.sql("""
                        INSERT INTO outbox_event (id, topic, aggregate_id, event_type, payload, trace_context)
                        VALUES (:id, :topic, :key, :eventType, CAST(:payload AS jsonb), CAST(:traceContext AS jsonb))
                        """)
                .param("id", UUID.randomUUID())
                .param("topic", topic)
                .param("key", key)
                .param("eventType", eventType)
                .param("payload", jsonMapper.writeValueAsString(payload))
                .param("traceContext", jsonMapper.writeValueAsString(currentTraceContext()))
                .update();
    }

    private Map<String, String> currentTraceContext() {
        Map<String, String> headers = new HashMap<>();
        TraceContext context = tracer.currentTraceContext().context();
        if (context != null) {
            propagator.inject(context, headers, Map::put);
        }
        return headers;
    }
}
