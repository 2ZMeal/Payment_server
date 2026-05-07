/*
package org.ezmeal.payment.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic paymentCompletedEventTopic() {
        return TopicBuilder.name("payment-completed-event-topic")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentCancelledEventTopic() {
        return TopicBuilder.name("payment-cancelled-event-topic")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name("payment-failed-event-topic")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
*/
