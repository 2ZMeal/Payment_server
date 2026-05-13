package org.ezmeal.payment.infrastructure.message.kafka.consumer;

import com.ezmeal.common.message.EventEnvelope;
import com.ezmeal.common.message.inbox.InboxProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ezmeal.payment.application.service.PaymentService;
import org.ezmeal.payment.domain.event.payload.OrderCancelledEvent;
import org.ezmeal.payment.domain.event.payload.OrderCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * [역할]
 * Order Service에서 발행하는 주문 이벤트를 처리하는 Consumer
 *
 * [처리 흐름]
 * 1. @KafkaListener로 Topic 구독
 * 2. EventEnvelope 형태로 자동 역직렬화
 * 3. InboxProcessor.processOnce()에 위임
 *    - eventId 기반 중복 수신 감지
 *    - 비즈니스 로직과 Inbox 저장을 하나의 @Transactional로 보장
 *
 * [Kafka Topic]
 * - order.created: 주문 생성 → 결제 생성
 * - order.cancelled: 주문 취소 → 결제 취소
 *
 * [Consumer Group]
 * payment-service-group
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final InboxProcessor inboxProcessor;
    private final PaymentService paymentService;

    /**
     * [구독 Topic]
     * order.created
     *
     * [처리 로직]
     * 1. EventEnvelope 자동 역직렬화
     * 2. InboxProcessor로 중복 체크
     * 3. PaymentService.createPaymentFromOrder() 호출
     *    - Payment 엔티티 생성
     *    - status: READY
     * 4. Inbox에 처리 기록 저장
     */
    @KafkaListener(
            topics = "order.created",
            groupId = "payment-service-group"
    )
    public void handleOrderCreatedEvent(EventEnvelope<OrderCreatedEvent> event) {
        try {
            log.info("[Kafka Listener] 주문 생성 이벤트 수신: eventId={}, orderId={}, userId={}, totalPrice={}",
                    event.eventId(),
                    event.payload().getOrderId(),
                    event.payload().getUserId(),
                    event.payload().getTotalPrice());

            // ✅ InboxProcessor: 멱등성 보장
            inboxProcessor.processOnce(event.eventId(), () -> {
                OrderCreatedEvent payload = event.payload();
                log.info("[비즈니스 로직] 결제 생성: orderId={}, userId={}, totalPrice={}",
                        payload.getOrderId(),
                        payload.getUserId(),
                        payload.getTotalPrice());

                // Payment 생성
                paymentService.createPaymentFromOrder(
                        payload.getOrderId(),
                        UUID.fromString(payload.getUserId()),
                        payload.getTotalPrice()

                         );

                log.info("[비즈니스 로직] 결제 생성 완료: orderId={}", payload.getOrderId());
            });

        } catch (Exception e) {
            log.error("[Kafka Listener] 주문 생성 이벤트 처리 실패", e);
            throw new RuntimeException("주문 생성 이벤트 처리 실패", e);
        }
    }

    /**
     * [구독 Topic]
     * order.cancelled
     *
     * [처리 로직]
     * 1. EventEnvelope 자동 역직렬화
     * 2. InboxProcessor로 중복 체크
     * 3. requiresPaymentCancellation 확인
     *    - true: PaymentService.cancelPaymentFromOrder() 호출
     *    - false: 취소 처리 스킵 (결제가 없었음)
     * 4. Inbox에 처리 기록 저장
     */
    @KafkaListener(
            topics = "order.cancelled",
            groupId = "payment-service-group"
    )
    public void handleOrderCancelledEvent(EventEnvelope<OrderCancelledEvent> event) {
        try {
            log.info("[Kafka Listener] 주문 취소 이벤트 수신: eventId={}, orderId={}, userId={}, requiresPaymentCancellation={}",
                    event.eventId(),
                    event.payload().getOrderId(),
                    event.payload().getUserId(),
                    event.payload().isRequiresPaymentCancellation());

            inboxProcessor.processOnce(event.eventId(), () -> {
                OrderCancelledEvent payload = event.payload();

                // 결제 취소가 필요한지 확인
                if (payload.isRequiresPaymentCancellation()) {
                    log.info("[비즈니스 로직] 결제 취소: orderId={}, cancelledBy={}",
                            payload.getOrderId(),
                            payload.getCancelledBy());

                    // Payment 취소
                    paymentService.cancelPaymentFromOrder(
                            payload.getOrderId(),
                            "Order cancelled by " + payload.getCancelledBy()
                    );

                    log.info("[비즈니스 로직] 결제 취소 완료: orderId={}", payload.getOrderId());
                } else {
                    log.info("[비즈니스 로직] 결제 취소 불필요: orderId={} (결제 전 취소)",
                            payload.getOrderId());
                }
            });

        } catch (Exception e) {
            log.error("[Kafka Listener] 주문 취소 이벤트 처리 실패", e);
            throw new RuntimeException("주문 취소 이벤트 처리 실패", e);
        }
    }
}