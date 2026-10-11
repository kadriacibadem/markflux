package com.markflux.common.messaging;

import java.time.OffsetDateTime;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

@Component
@Slf4j
@RequiredArgsConstructor
public class Inbox {

    private final JdbcClient jdbcClient;
    private final TransactionTemplate transactionTemplate;
    private final JsonMapper jsonMapper;

    public <T> void process(String consumer, IncomingEvent event, Class<T> payloadType, Consumer<T> handler) {
        // json to payload
        T payload = jsonMapper.readValue(event.payload(), payloadType);
        transactionTemplate.executeWithoutResult(status -> {
            int inserted = jdbcClient.sql("""
                            INSERT INTO inbox_message (consumer, message_id)
                            VALUES (:consumer, :messageId)
                            ON CONFLICT DO NOTHING
                            """)
                    .param("consumer", consumer)
                    .param("messageId", event.id())
                    .update();
            if (inserted == 0) {
                log.info("Skipping duplicate {} event {}", event.type(), event.id());
                return;
            }
            handler.accept(payload);
        });
    }

    @Scheduled(cron = "0 30 * * * *")
    public void deleteOldMessages() {
        int deleted = jdbcClient.sql("DELETE FROM inbox_message WHERE processed_at < :threshold")
                .param("threshold", OffsetDateTime.now().minusDays(14))
                .update();
        if (deleted > 0) {
            log.info("Deleted {} old inbox messages", deleted);
        }
    }
}
