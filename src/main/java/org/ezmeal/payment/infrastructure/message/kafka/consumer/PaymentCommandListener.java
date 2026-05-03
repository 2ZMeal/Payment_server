/*package org.ezmeal.payment.infrastructure.message.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ezmeal.payment.application.service.PaymentService;
import org.ezmeal.payment.domain.event.PaymentEventProducer;
import org.ezmeal.payment.domain.event.payload.PaymentCancelledEvent;
import org.ezmeal.payment.domain.event.payload.PaymentCompletedEvent;
import org.ezmeal.payment.domain.model.Payment;
import org.ezmeal.payment.domain.repository.PaymentRepository;
import org.ezmeal.payment.infrastructure.message.kafka.event.command.PaymentCompleteCommand;
import org.ezmeal.payment.infrastructure.message.kafka.event.command.PaymentCancelCommand;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCommandListener {

    private final PaymentService paymentService;
    private final PaymentEventProducer paymentEventProducer;
    private final PaymentRepository paymentRepository;

    @KafkaListener(
            topics = "payment-complete-command-topic",
            groupId = "payment-service"
    )
    @Transactional
    public void handlePaymentCompleteCommand(@Payload PaymentCompleteCommand command) {

        log.info("[Kafka] 결제 완료 명령 수신: paymentId={}", command.getPaymentId());

        try {
            paymentService.completePayment(command.getPaymentId(), command.getPaymentKey(), command.getUserId());

            // ✅ Payment 저장 후 이벤트 발행
            Payment savedPayment = paymentRepository.findById(command.getPaymentId())
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

            PaymentCompletedEvent event = PaymentCompletedEvent.of(
                    savedPayment.getPaymentId(),
                    savedPayment.getOrderId(),
                    savedPayment.getUserId(),
                    savedPayment.getPrice(),
                    command.getPaymentKey()
            );

            paymentEventProducer.publishCompletedEvent(event);
            log.info("[Kafka] 결제 완료 이벤트 발행: paymentId={}", command.getPaymentId());

        } catch (Exception e) {
            log.error("[Kafka] 결제 완료 처리 오류: {}", e.getMessage());
        }
    }

    @KafkaListener(
            topics = "payment-cancel-command-topic",
            groupId = "payment-service"
    )
    @Transactional
    public void handlePaymentCancelCommand(@Payload PaymentCancelCommand command) {

        log.info("[Kafka] 결제 취소 명령 수신: paymentId={}", command.getPaymentId());

        try {
            paymentService.cancelPayment(command.getPaymentId(), command.getReason(), command.getUserId());

            // ✅ Payment 저장 후 이벤트 발행
            Payment savedPayment = paymentRepository.findById(command.getPaymentId())
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

            PaymentCancelledEvent event = PaymentCancelledEvent.of(
                    savedPayment.getPaymentId(),
                    savedPayment.getOrderId(),
                    savedPayment.getUserId(),
                    savedPayment.getPrice(),
                    command.getReason()
            );

            paymentEventProducer.publishCancelledEvent(event);
            log.info("[Kafka] 결제 취소 이벤트 발행: paymentId={}", command.getPaymentId());

        } catch (Exception e) {
            log.error("[Kafka] 결제 취소 처리 오류: {}", e.getMessage());
        }
    }
}
*/
