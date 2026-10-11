package com.markflux.common.messaging;

import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

public record IncomingEvent(String id, String type, String payload) {

    public static IncomingEvent from(ConsumerRecord<String, String> record) {
        String id = header(record, EventHeaders.ID);
        String type = header(record, EventHeaders.TYPE);
        if (id == null || type == null || record.value() == null) {
            throw new IllegalArgumentException("Invalid event at %s-%d offset %d"
                    .formatted(record.topic(), record.partition(), record.offset()));
        }
        return new IncomingEvent(id, type, record.value());
    }

    private static String header(ConsumerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);
        if (header == null || header.value() == null) {
            return null;
        }
        String value = new String(header.value(), StandardCharsets.UTF_8);
        return value.isBlank() ? null : value;
    }
}
