package org.ezmeal.payment.domain.event.payload;

import com.ezmeal.common.message.DomainEvent;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PaymentCancelledEvent implements DomainEvent, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;


    private UUID paymentId;
    private UUID orderId;
    private UUID userId;
    private String status;
    private Integer amount;
    private String reason;

    public static PaymentCancelledEvent of(
            UUID paymentId,
            UUID orderId,
            UUID userId,
            Integer amount,
            String reason

    ) {
        return PaymentCancelledEvent.builder()


                .paymentId(paymentId)
                .orderId(orderId)
                .userId(userId)
                .status("CANCELLED")
                .amount(amount)
                .reason(reason)

                .build();
    }
}
