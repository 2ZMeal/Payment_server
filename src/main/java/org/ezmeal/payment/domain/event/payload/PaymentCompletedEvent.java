package org.ezmeal.payment.domain.event.payload;

import com.ezmeal.common.message.DomainEvent;
import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PaymentCompletedEvent implements DomainEvent, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private UUID eventId;
    private OffsetDateTime occurredAt;
    private UUID paymentId;
    private UUID orderId;
    private UUID userId;
    private String status;
    private Integer amount;
    private String paymentKey;

    public static PaymentCompletedEvent of(
            UUID paymentId,
            UUID orderId,
            UUID userId,
            Integer amount,
            String paymentKey

    ) {
        return PaymentCompletedEvent.builder()
                .eventId(UUID.randomUUID())
                .occurredAt(OffsetDateTime.now())
                .paymentId(paymentId)
                .orderId(orderId)
                .userId(userId)
                .status("COMPLETED")
                .amount(amount)
                .paymentKey(paymentKey)
                .build();
    }
}
