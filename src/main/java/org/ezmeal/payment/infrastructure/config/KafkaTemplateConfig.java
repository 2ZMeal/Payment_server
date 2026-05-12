// package org.ezmeal.payment.infrastructure.config;
//
// import lombok.RequiredArgsConstructor;
// import org.ezmeal.payment.domain.event.payload.PaymentCancelledEvent;
// import org.ezmeal.payment.domain.event.payload.PaymentCompletedEvent;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.kafka.core.KafkaTemplate;
// import org.springframework.kafka.core.ProducerFactory;
//
// /**
//  * [비활성화 사유]
//  * 공통 모듈(com.ezmeal:common)의 KafkaProducerConfig에서
//  * 범용 KafkaTemplate<String, EventEnvelope<? extends DomainEvent>> 제공
//  *
//  * 기존 개별 Bean 방식의 문제점:
//  * - 이벤트 타입마다 새로운 Template Bean 필요
//  * - 설정 중복 (acks, retries 등이 각 Bean에서 반복)
//  * - 확장성 부족
//  *
//  * 공통 모듈 통합 후:
//  * - 모든 이벤트가 동일한 KafkaTemplate 사용
//  * - application.yaml의 spring.kafka 설정으로 중앙화
//  * - EventEnvelope로 메타데이터 자동 추가
//  * - SecurityInterceptor 자동 적용
//  */
// @Configuration
// @RequiredArgsConstructor
// public class KafkaTemplateConfig {
//
//     // PaymentCompletedEvent용 KafkaTemplate
//     @Bean
//     public KafkaTemplate<String, PaymentCompletedEvent> completedEventKafkaTemplate(
//             ProducerFactory<String, PaymentCompletedEvent> producerFactory) {
//         KafkaTemplate<String, PaymentCompletedEvent> template = new KafkaTemplate<>(producerFactory);
//         template.setObservationEnabled(true);  // ← Micrometer tracing 활성화
//         return template;
//     }
//
//     // PaymentCancelledEvent용 KafkaTemplate
//     @Bean
//     public KafkaTemplate<String, PaymentCancelledEvent> cancelledEventKafkaTemplate(
//             ProducerFactory<String, PaymentCancelledEvent> producerFactory) {
//         KafkaTemplate<String, PaymentCancelledEvent> template = new KafkaTemplate<>(producerFactory);
//         template.setObservationEnabled(true);  // ← Micrometer tracing 활성화
//         return template;
//     }
//
// }