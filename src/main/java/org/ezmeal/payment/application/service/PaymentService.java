package org.ezmeal.payment.application.service;

import java.util.List;
import java.util.UUID;
import org.ezmeal.payment.application.dto.request.PaymentRequestDto;
import org.ezmeal.payment.application.dto.request.TossConfirmRequest;
import org.ezmeal.payment.application.dto.response.PaymentResponseDto;

public interface PaymentService {
    // 1. 결제 요청 생성 (주문 시 호출)
    PaymentResponseDto createPayment(PaymentRequestDto requestDto, UUID authenticatedUserId);

    // 2. 결제 승인 처리 (PG사 인증 완료 후 호출)
    PaymentResponseDto approvePayment(TossConfirmRequest request, UUID currentUserId);

    //  3. 결제 단건 조회
    PaymentResponseDto getPaymentDetail(UUID paymentId);

    //  4. 결제 목록 조회
    List<PaymentResponseDto> getPaymentList(UUID currentUserId, String roles);

    // 5. 결제 취소
    PaymentResponseDto cancelPayment(UUID paymentId, String cancelReason, UUID currentUserId);

}




