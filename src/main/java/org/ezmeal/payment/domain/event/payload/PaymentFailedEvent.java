package org.ezmeal.payment.domain.event.payload;

import com.ezmeal.common.message.DomainEvent;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder

public class PaymentFailedEvent implements DomainEvent, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private UUID eventId;
    private OffsetDateTime occurredAt;
    private UUID paymentId;
    private UUID orderId;
    private UUID userId;
    private String status;        // "FAILED"
    private Integer amount;
    private String failureReason; // "카드 한도 부족", "네트워크 에러" 등
    private String errorCode;     // "INSUFFICIENT_BALANCE", "NETWORK_ERROR" 등

    public static PaymentFailedEvent of(



            UUID paymentId,
            UUID orderId,
            UUID userId,
            Integer amount,
            String failureReason,
            String errorCode

    ) {
        return  PaymentFailedEvent.builder()
                .eventId(UUID.randomUUID())
                .occurredAt(OffsetDateTime.now())
                .paymentId(paymentId)
                .orderId(orderId)
                .userId(userId)
                .status("FAILED")
                .amount(amount)
                .failureReason(failureReason)
                .errorCode(errorCode)
                .build();


    }
}