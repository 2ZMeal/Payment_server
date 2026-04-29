package org.ezmeal.payment.domain.exception;

import com.ezmeal.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAY-001", "결제 내역을 찾을 수 없습니다."),
    PAYMENT_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "PAY-002", "이미 완료된 결제입니다."),
    PG_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "PAY-003", "PG사 통신 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
