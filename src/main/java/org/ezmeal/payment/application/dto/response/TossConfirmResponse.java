package org.ezmeal.payment.application.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TossConfirmResponse {
    private String paymentKey;
    private UUID orderId;
    private String status;      // DONE, CANCELED 등
    private String approvedAt;  // 승인 시점
    private Long totalAmount;
}
