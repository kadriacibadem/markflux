package com.markflux.common.messaging;

import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.core.JacksonException;

@Configuration
@EnableScheduling
public class MessagingConfig {

    @Bean
    public DefaultKafkaProducerFactoryCustomizer kafkaProducerSettings() {
        return factory -> factory.updateConfigs(Map.of(
                ProducerConfig.ACKS_CONFIG, "all",
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true,
                ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 10_000,
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30_000));
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate,
            @Value("${spring.kafka.consumer.group-id:${spring.application.name}}") String groupId) {
        DeadLetterPublishingRecoverer deadLetter = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + "." + groupId + ".dlt", -1));

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(4);
        backOff.setInitialInterval(500);
        backOff.setMultiplier(2);
        backOff.setMaxInterval(10_000);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(deadLetter, backOff);
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class, JacksonException.class);
        return errorHandler;
    }
}
