package org.ezmeal.payment.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * [역할]
 * Spring Boot 애플리케이션 시작 시 Kafka Topic을 자동 생성
 *
 * [왜 필요한가?]
 * - 수동으로 Topic 생성 불필요 (자동화)
 * - 개발 환경에서 Kafka 셋업 간소화
 * - 운영 환경에서도 Topic 존재 여부 보장
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic paymentCompletedEventTopic() {
        return TopicBuilder.name("payment.completed")
                .partitions(1)          // 단일 파티션 (순서 보장)
                .replicas(1)            // 로컬 개발용 (운영: 3 이상 권장)
                .build();
    }

    @Bean
    public NewTopic paymentCancelledEventTopic() {
        return TopicBuilder.name("payment.cancelled")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name("payment.failed")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
