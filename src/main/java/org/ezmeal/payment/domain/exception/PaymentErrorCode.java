package org.ezmeal.payment.domain.exception;

import com.ezmeal.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAY-404", "결제 내역을 찾을 수 없습니다."),
    INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "PAY-200", "결제 금액이 일치하지 않습니다."),
    ALREADY_PROCESSED_PAYMENT(HttpStatus.CONFLICT, "PAY-300", "이미 처리된 결제건입니다."),
    PAYMENT_GATEWAY_ERROR(HttpStatus.BAD_GATEWAY, "PAY-400", "외부 결제사 통신 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
