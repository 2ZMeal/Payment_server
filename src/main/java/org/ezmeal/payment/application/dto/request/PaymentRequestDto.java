package org.ezmeal.payment.application.dto.request;

import lombok.Builder;
import lombok.Getter;
import java.util.UUID;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.ezmeal.payment.domain.model.enums.PaymentMethod;
import org.ezmeal.payment.domain.model.enums.PgProvider;

@Getter
@Builder
// NoARgsConstructor와 AllArgs.. 추가
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {

    private UUID orderId;
    private Integer price;
    private Integer totalPrice;
    private PgProvider pgProvider;
    private PaymentMethod paymentMethod;
}
