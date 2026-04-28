package org.ezmeal.payment.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmRequestDto {

    // PG사에서 발급해준 결제 고유 키
    private String paymentKey;

    // 우리가 생성했던 주문 번호
    private String orderId;

    // 실제로 결제된 금액 (변조 확인용)
    private Integer amount;
}
