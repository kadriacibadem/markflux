package com.markflux.common.messaging;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Component
@Slf4j
public class OutboxRelay {

    //postgresql lock id
    private static final long RELAY_LOCK_ID = 7_061_726_565_726_179L;
    private static final int BATCH_SIZE = 100;

    private final JdbcClient jdbcClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionTemplate transactionTemplate;
    private final JsonMapper jsonMapper;
    private final Tracer tracer;
    private final Propagator propagator;
    private final String source;

    public OutboxRelay(JdbcClient jdbcClient, KafkaTemplate<String, String> kafkaTemplate,
            TransactionTemplate transactionTemplate, JsonMapper jsonMapper, ObjectProvider<Tracer> tracer,
            ObjectProvider<Propagator> propagator, @Value("${spring.application.name}") String applicationName) {
        this.jdbcClient = jdbcClient;
        this.kafkaTemplate = kafkaTemplate;
        this.transactionTemplate = transactionTemplate;
        this.jsonMapper = jsonMapper;
        this.tracer = tracer.getIfAvailable(() -> Tracer.NOOP);
        this.propagator = propagator.getIfAvailable(() -> Propagator.NOOP);
        this.source = "/" + applicationName;
    }

    @Scheduled(fixedDelayString = "${markflux.outbox.poll-interval-ms:500}")
    public void publishPendingEvents() {
        try {
            transactionTemplate.executeWithoutResult(status -> publishBatch());
        } catch (RuntimeException ex) {
            log.warn("Outbox relay failed", ex);
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void deletePublishedEvents() {
        int deleted = jdbcClient.sql("DELETE FROM outbox_event WHERE published_at < :threshold")
                .param("threshold", OffsetDateTime.now().minusDays(7))
                .update();
        if (deleted > 0) {
            log.info("Deleted {} published outbox events", deleted);
        }
    }

    private void publishBatch() {
        // postgre lock
        boolean locked = jdbcClient.sql("SELECT pg_try_advisory_xact_lock(:id)")
                .param("id", RELAY_LOCK_ID)
                .query(Boolean.class)
                .single();
        if (!locked) {
            return;
        }
        List<OutboxEvent> events = jdbcClient.sql("""
                        SELECT id, topic, aggregate_id, event_type, payload::text, trace_context::text, created_at
                        FROM outbox_event
                        WHERE published_at IS NULL
                        ORDER BY sequence_number
                        LIMIT :limit
                        """)
                .param("limit", BATCH_SIZE)
                .query((rs, rowNum) -> new OutboxEvent(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getObject(7, OffsetDateTime.class)))
                .list();
        for (OutboxEvent event : events) {
            try {
                send(event);
            } catch (Exception ex) {
                log.warn("Could not publish {} event {}, will retry", event.type(), event.id(), ex);
                jdbcClient.sql("UPDATE outbox_event SET attempts = attempts + 1, last_error = :error WHERE id = :id")
                        .param("error", String.valueOf(ex.getMessage()))
                        .param("id", event.id())
                        .update();
                return;
            }
            jdbcClient.sql("UPDATE outbox_event SET published_at = now() WHERE id = :id")
                    .param("id", event.id())
                    .update();
        }
    }

    private void send(OutboxEvent event) throws Exception {
        ProducerRecord<String, String> record = new ProducerRecord<>(event.topic(), event.key(), event.payload());
        addHeader(record, EventHeaders.SPEC_VERSION, "1.0");
        addHeader(record, EventHeaders.ID, event.id().toString());
        addHeader(record, EventHeaders.TYPE, event.type());
        addHeader(record, EventHeaders.SOURCE, source);
        addHeader(record, EventHeaders.TIME, event.createdAt().toInstant().toString());

        // create span for tracing
        Map<String, String> traceContext = jsonMapper.readValue(event.traceContext(), new TypeReference<>() {
        });
        Span span = propagator.extract(traceContext, Map::get).name("outbox " + event.type()).start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            kafkaTemplate.send(record).get();
        } finally {
            span.end();
        }
    }

    private static void addHeader(ProducerRecord<String, String> record, String name, String value) {
        record.headers().add(name, value.getBytes(StandardCharsets.UTF_8));
    }

    private record OutboxEvent(UUID id, String topic, String key, String type, String payload, String traceContext,
            OffsetDateTime createdAt) {
    }
}
