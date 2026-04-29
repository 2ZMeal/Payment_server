package org.ezmeal.payment.application.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.ezmeal.payment.domain.model.Payment;
import org.ezmeal.payment.domain.model.enums.PaymentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentResponseDto {

    private UUID paymentId;
    private UUID orderId;
    private PaymentStatus status;
    private Integer price;
    private String pgTransactionId;
    private String paymentKey;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    // Entity -> DTO 변환 메서드 (Static Factory Method)
    public static PaymentResponseDto from(Payment payment) {
        return PaymentResponseDto.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .status(payment.getStatus())
                .price(payment.getPrice())
                .pgTransactionId(payment.getPgTransactionId())
                .paymentKey(payment.getPaymentKey())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt()) // BaseEntity에서 물려받은 필드
                .build();
    }
}
