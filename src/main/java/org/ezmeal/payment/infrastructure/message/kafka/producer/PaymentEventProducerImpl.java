package org.ezmeal.payment.infrastructure.message.kafka.producer;

import com.ezmeal.common.message.CommonKafkaEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ezmeal.payment.domain.event.PaymentEventProducer;
import org.ezmeal.payment.domain.event.payload.PaymentCancelledEvent;
import org.ezmeal.payment.domain.event.payload.PaymentCompletedEvent;
import org.ezmeal.payment.domain.event.payload.PaymentFailedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
/**
 * [리팩토링 사항]
 * 공통 모듈(com.ezmeal:common)의 CommonKafkaEventPublisher로 통합

 * [변경 전 문제점]
 * 1. 이벤트별 개별 KafkaTemplate 사용 (코드 중복)
 * 2. 헤더 정보 수동 관리 (코드 복잡)
 * 3. Topic 이름 하드코딩 (일관성 부족)
 * 4. 데이터 손실 위험 (Kafka 발행 실패 시)
 * 5. 순서 보장 불가 (eventId를 key로 사용)

 * [변경 후 개선점]
 * ✅ Outbox 패턴: DB 트랜잭션과 Kafka 발행 보장
 * ✅ EventEnvelope: 메타데이터 자동 추가
 * ✅ 보안: SecurityInterceptor로 헤더 자동 관리
 * ✅ 순서 보장: aggregateId (paymentId)를 key로 사용
 * ✅ 확장성: 새로운 이벤트 추가 시 자동 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducerImpl implements PaymentEventProducer {

    private final CommonKafkaEventPublisher eventPublisher;

    @Override
    @Transactional
    public void publishCompletedEvent(PaymentCompletedEvent event) {
        log.info("[Kafka] 결제 완료 이벤트 발행: paymentId={}, orderId={}", 
                event.getPaymentId(), event.getOrderId());

        try {
            eventPublisher.publish(
                    "payment.completed",      // Topic name
                    event.getPaymentId().toString(),      // aggregateId (순서 보장용)
                    "PAYMENT_COMPLETED",                  // eventType
                    event                                 // payload (EventEnvelope로 자동 감싸짐)
            );

            log.info("[Kafka] 결제 완료 이벤트 발행 완료: paymentId={}", event.getPaymentId());
        } catch (Exception e) {
            log.error("[Kafka] 결제 완료 이벤트 발행 실패: paymentId={}", event.getPaymentId(), e);
            throw new RuntimeException("결제 완료 이벤트 발행 실패", e);
        }
    }

    @Override
    @Transactional
    public void publishCancelledEvent(PaymentCancelledEvent event) {
        log.info("[Kafka] 결제 취소 이벤트 발행: paymentId={}, orderId={}", 
                event.getPaymentId(), event.getOrderId());

        try {
            eventPublisher.publish(
                    "payment.cancelled",      // Topic name
                    event.getPaymentId().toString(),      // aggregateId (순서 보장용)
                    "PAYMENT_CANCELLED",                  // eventType
                    event                                 // payload (EventEnvelope로 자동 감싸짐)
            );

            log.info("[Kafka] 결제 취소 이벤트 발행 완료: paymentId={}", event.getPaymentId());
        } catch (Exception e) {
            log.error("[Kafka] 결제 취소 이벤트 발행 실패: paymentId={}", event.getPaymentId(), e);
            throw new RuntimeException("결제 취소 이벤트 발행 실패", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishFailedEvent(PaymentFailedEvent event){

        log.info("[kafka] 결제 실패 이벤트 발행: paymentId={}, orderId={}", event.getPaymentId(),event.getOrderId());

        try {
            eventPublisher.publish(
                    "payment.failed",
                    event.getPaymentId().toString(),
                    "PAYMENT_FAILED",
                    event
            );

            log.info("[Kafka] 결제 실패 이벤트 발행 완료: paymentId={}", event.getPaymentId());
        }catch (Exception e){
            log.error("[Kafka] 결제 실패 이벤트발행:paymentId={} ", event.getPaymentId(), e);
            throw new RuntimeException("결제 실패 이벤트 발행 !", e);
        }

    }



}
