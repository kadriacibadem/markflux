package com.markflux.product.config;

import com.markflux.product.messaging.ProductEventPublisher;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic productEventsTopic() {
        return TopicBuilder.name(ProductEventPublisher.TOPIC).partitions(3).build();
    }
}
