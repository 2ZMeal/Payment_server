package org.ezmeal.payment.infrastructure.message.kafka.consumer;

import com.ezmeal.common.message.EventEnvelope;
import com.ezmeal.common.message.inbox.InboxProcessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ezmeal.payment.domain.event.payload.PaymentCancelledEvent;
import org.ezmeal.payment.domain.event.payload.PaymentCompletedEvent;
import org.ezmeal.payment.domain.event.payload.PaymentFailedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * [역할]
 * Kafka에서 수신한 Payment 관련 이벤트를 처리하는 Consumer
 *
 * [처리 흐름]
 * 1. @KafkaListener로 Topic 구독
 * 2. EventEnvelope 형태로 자동 역직렬화 (공통 모듈 StringJsonMessageConverter)
 * 3. InboxProcessor.processOnce()에 위임
 *    - eventId 기반 중복 수신 감지
 *    - 비즈니스 로직과 Inbox 저장을 하나의 @Transactional로 보장
 *
 * [왜 InboxProcessor를 사용하는가?]
 * Kafka 메시지 재전송 시 멱등성 보장
 * 예) 같은 이벤트가 2번 수신됨
 *    1️⃣ 첫 번째: Inbox에 eventId 저장 ✓
 *    2️⃣ 두 번째: Inbox 중복 감지 → 조기 종료 ✓ (중복 처리 방지)
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final InboxProcessor inboxProcessor;
    private final ObjectMapper objectMapper;

    /**
     * [구독 Topic]
     * payment-completed-event-topic
     * - KafkaTopicConfig.paymentCompletedEventTopic()에서 생성
     *
     * [Consumer Group]
     * payment-service-group
     * - application.yaml의 spring.kafka.consumer.group-id 설정
     *
     * [메시지 형식]
     * EventEnvelope<PaymentCompletedEvent>
     * - eventId: 중복 제거용 고유 ID
     * - eventType: "PAYMENT_COMPLETED"
     * - aggregateId: paymentId (순서 보장)
     * - occurredAt: 발행 시간
     * - payload: 실제 PaymentCompletedEvent 데이터
     */
    @KafkaListener(
            topics = "payment-completed",
            groupId = "payment-service-group"
    )
    public void handlePaymentCompletedEvent(String message) {
        try {
            EventEnvelope<PaymentCompletedEvent> event = objectMapper.readValue(
                    message,
                    new TypeReference<EventEnvelope<PaymentCompletedEvent>>() {}
            );

            log.info("[Kafka Listener] 결제 완료 이벤트 수신: eventId={}, paymentId={}, orderId={}",
                    event.eventId(),
                    event.payload().getPaymentId(),
                    event.payload().getOrderId());

            // ✅ InboxProcessor: 멱등성 보장
            // - eventId를 UNIQUE 키로 사용하여 중복 수신 방지
            // - 비즈니스 로직과 Inbox Insert를 같은 @Transactional로 보장
            // - 실패 시 모두 롤백, 성공 시 모두 커밋
            inboxProcessor.processOnce(event.eventId(), () -> {
                PaymentCompletedEvent payload = event.payload();
                log.info("[비즈니스 로직] 결제 완료 처리: orderId={}, amount={}, userId={}",
                        payload.getOrderId(),
                        payload.getAmount(),
                        payload.getUserId());

                // TODO: Order Service 호출하여 주문 상태 업데이트
                // orderService.completePayment(
                //     orderId = payload.getOrderId(),
                //     paymentId = payload.getPaymentId(),
                //     amount = payload.getAmount()
                // );
            });
        } catch (Exception e) {
            log.error("[Kafka Listener] 결제 완료 이벤트 처리 실패", e);
            throw new RuntimeException("결제 완료 이벤트 처리 실패", e);
        }
    }

    /**
     * [구독 Topic]
     * payment-cancelled-event-topic
     */
    @KafkaListener(
            topics = "payment.cancelled",
            groupId = "payment-service-group"
    )
    public void handlePaymentCancelledEvent(String message) {
        try {
            EventEnvelope<PaymentCancelledEvent> event = objectMapper.readValue(
                    message,
                    new TypeReference<EventEnvelope<PaymentCancelledEvent>>() {}
            );

            log.info("[Kafka Listener] 결제 취소 이벤트 수신: eventId={}, paymentId={}, orderId={}",
                    event.eventId(),
                    event.payload().getPaymentId(),
                    event.payload().getOrderId());

            inboxProcessor.processOnce(event.eventId(), () -> {
                PaymentCancelledEvent payload = event.payload();
                log.info("[비즈니스 로직] 결제 취소 처리: orderId={}, reason={}, userId={}",
                        payload.getOrderId(),
                        payload.getReason(),
                        payload.getUserId());

//                 TODO: Order Service 호출하여 주문 상태 업데이트
//                 orderService.cancelPayment(
//                     orderId = payload.getOrderId(),
//                     paymentId = payload.getPaymentId(),
//                     reason = payload.getReason()
//                 );
            });
        } catch (Exception e) {
            log.error("[Kafka Listener] 결제 취소 이벤트 처리 실패", e);
            throw new RuntimeException("결제 취소 이벤트 처리 실패", e);
        }
    }
    /**
     * [구독 Topic]
     * payment-failed-event-topic
     */
    @KafkaListener(
            topics = "payment-failed-event-topic",
            groupId = "payment-service-group"
    )
    public void handlePaymentFailedEvent(String message) {
        try {
            EventEnvelope<PaymentFailedEvent> event = objectMapper.readValue(
                    message,
                    new TypeReference<EventEnvelope<PaymentFailedEvent>>() {}
            );

            log.info("[Kafka Listener] 결제 실패 이벤트 수신: eventId={}, paymentId={}, orderId={}, reason={}",
                    event.eventId(),
                    event.payload().getPaymentId(),
                    event.payload().getOrderId(),
                    event.payload().getFailureReason());

            inboxProcessor.processOnce(event.eventId(), () -> {
                PaymentFailedEvent payload = event.payload();
                log.info("[비즈니스 로직] 결제 실패 처리: orderId={}, reason={}, errorCode={}, userId={}",
                        payload.getOrderId(),
                        payload.getFailureReason(),
                        payload.getErrorCode(),
                        payload.getUserId());

                // TODO: Order Service 호출하여 주문 상태 업데이트
                // orderService.failPayment(
                //     orderId = payload.getOrderId(),
                //     paymentId = payload.getPaymentId(),
                //     failureReason = payload.getFailureReason()
                // );
            });
        } catch (Exception e) {
            log.error("[Kafka Listener] 결제 실패 이벤트 처리 실패", e);
            throw new RuntimeException("결제 실패 이벤트 처리 실패", e);
        }
    }






}
