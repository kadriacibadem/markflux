package com.markflux.inventory.config;

import com.markflux.inventory.messaging.InventoryEventPublisher;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic inventoryEventsTopic() {
        return TopicBuilder.name(InventoryEventPublisher.TOPIC).partitions(3).build();
    }
}
