package org.ezmeal.payment.application.dto.request;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TossConfirmRequest {
    private String paymentKey;
    private UUID orderId;
    private Integer amount;
}
