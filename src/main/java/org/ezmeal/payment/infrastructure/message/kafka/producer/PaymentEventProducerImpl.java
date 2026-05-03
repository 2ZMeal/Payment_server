package org.ezmeal.payment.infrastructure.message.kafka.producer;

import com.ezmeal.common.security.principal.CustomUserPrincipal;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.ezmeal.payment.domain.event.PaymentEventProducer;
import org.ezmeal.payment.domain.event.payload.PaymentCancelledEvent;
import org.ezmeal.payment.domain.event.payload.PaymentCompletedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducerImpl implements PaymentEventProducer {

    private final KafkaTemplate<String, PaymentCompletedEvent> completedEventKafkaTemplate;
    private final KafkaTemplate<String, PaymentCancelledEvent> cancelledEventKafkaTemplate;

    @Override
    public void publishCompletedEvent(PaymentCompletedEvent event) {
        log.info("[Kafka] 결제 완료 이벤트 발행: paymentId={}", event.getPaymentId());

        ProducerRecord<String, PaymentCompletedEvent> record =
                new ProducerRecord<>(
                        "payment-result",
                        event.getEventId().toString(),
                        event
                );

        // ✅ SecurityContext에서 먼저 시도
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() &&
                auth.getPrincipal() instanceof CustomUserPrincipal principal) {

            // SecurityContext에서 헤더 추가
            record.headers().add(
                    new RecordHeader("X-User-Id", principal.getUserId().getBytes(StandardCharsets.UTF_8))
            );
            record.headers().add(
                    new RecordHeader("X-User-Roles", principal.getRole().name().getBytes(StandardCharsets.UTF_8))
            );
            String email = principal.getEmail() != null ? principal.getEmail() : "";
            record.headers().add(
                    new RecordHeader("X-User-Email", email.getBytes(StandardCharsets.UTF_8))
            );
        }

        completedEventKafkaTemplate.send(record)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] 결제 완료 이벤트 발행 실패", ex);
                        return;
                    }
                    log.info("[Kafka] 결제 완료 이벤트 발행 성공: topic={}, partition={}, offset={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );
                });
    }

    @Override
    public void publishCancelledEvent(PaymentCancelledEvent event) {
        log.info("[Kafka] 결제 취소 이벤트 발행: paymentId={}", event.getPaymentId());

        ProducerRecord<String, PaymentCancelledEvent> record =
                new ProducerRecord<>(
                        "payment-cancelled-event-topic",
                        event.getEventId().toString(),
                        event
                );

        // ✅ SecurityContext에서 먼저 시도
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() &&
                auth.getPrincipal() instanceof CustomUserPrincipal principal) {

            // SecurityContext에서 헤더 추가
            record.headers().add(
                    new RecordHeader("X-User-Id", principal.getUserId().getBytes(StandardCharsets.UTF_8))
            );
            record.headers().add(
                    new RecordHeader("X-User-Roles", principal.getRole().name().getBytes(StandardCharsets.UTF_8))
            );
            String email = principal.getEmail() != null ? principal.getEmail() : "";
            record.headers().add(
                    new RecordHeader("X-User-Email", email.getBytes(StandardCharsets.UTF_8))
            );
        }

        cancelledEventKafkaTemplate.send(record)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] 결제 취소 이벤트 발행 실패", ex);
                        return;
                    }
                    log.info("[Kafka] 결제 취소 이벤트 발행 성공: topic={}, partition={}, offset={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );
                });
    }
}
