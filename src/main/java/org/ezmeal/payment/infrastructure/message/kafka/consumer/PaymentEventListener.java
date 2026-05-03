//package org.ezmeal.payment.infrastructure.message.kafka.consumer;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.stereotype.Component;
//import java.util.UUID;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class PaymentEventListener {
//
//    @KafkaListener(
//            topics = "order.created",
//            groupId = "payment-service"
//    )
//    public void handleOrderCreatedEvent(@Payload UUID orderId) {
//        log.info("[Kafka] 주문 생성 이벤트 수신: orderId={}", orderId);
//
//        // Order Service에서 주문이 생성되었으니
//        // Payment Service에서 해야 할 작업 (예: 결제 준비, 예약금 처리 등)
//    }
//}
//
