package org.ezmeal.payment.application.service;

import org.ezmeal.payment.application.dto.request.PaymentRequestDto;
import org.ezmeal.payment.application.dto.response.PaymentResponseDto;

import java.util.UUID;

public interface PaymentService {
    // 1. 결제 요청 생성 (주문 시 호출)
    PaymentResponseDto createPayment(PaymentRequestDto requestDto);

    // 2. 결제 승인 처리 (PG사 인증 완료 후 호출)
    PaymentResponseDto approvePayment(String paymentKey, String orderId, Integer amount);
}
