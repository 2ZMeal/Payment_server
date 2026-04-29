package org.ezmeal.payment.domain.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentMethod {



    CARD("카드 결제", PgProvider.CARD),

    TOSS("토스페이", PgProvider.TOSS),

    KAKAO("카카오페이",PgProvider.KAKAO);



    private final String description;
    private final PgProvider validProvider;

}
