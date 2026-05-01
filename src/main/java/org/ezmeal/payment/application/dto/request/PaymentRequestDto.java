package org.ezmeal.payment.application.dto.request;

import lombok.Builder;
import lombok.Getter;
import java.util.UUID;
import org.ezmeal.payment.domain.model.enums.PaymentMethod;
import org.ezmeal.payment.domain.model.enums.PgProvider;

@Getter
@Builder
public class PaymentRequestDto {

    private UUID orderId;
    private Integer price;
    private Integer totalPrice;
    private PgProvider pgProvider;
    private PaymentMethod paymentMethod;
}
