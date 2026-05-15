package org.ezmeal.payment.application.service;

import org.ezmeal.payment.application.dto.request.PaymentRequestDto;
import org.ezmeal.payment.application.dto.request.TossConfirmRequest;
import org.ezmeal.payment.application.dto.response.PaymentResponseDto;

import java.util.List;
import java.util.UUID;

public interface PaymentService {
    // 1. 결제 요청 생성 (주문 시 호출)
    PaymentResponseDto createPayment(PaymentRequestDto requestDto, UUID authenticatedUserId);

    // 2. 결제 승인 처리 (PG사 인증 완료 후 호출)
    PaymentResponseDto approvePayment(TossConfirmRequest request, UUID currentUserId);

    //  3. 결제 단건 조회 (paymentId로)
    PaymentResponseDto getPaymentDetail(UUID paymentId);

    //  3-1. 결제 조회 (orderId로)
    PaymentResponseDto getPaymentByOrderId(UUID orderId);

    //  4. 결제 목록 조회
    List<PaymentResponseDto> getPaymentList(UUID currentUserId, String roles);

    // 5. 결제 취소
    PaymentResponseDto cancelPayment(UUID paymentId, String cancelReason, UUID currentUserId);

    //  추가 (Kafka에서 호출용)
    void completePayment(UUID paymentId, String paymentKey, UUID currentUserId);


    // Order Service 이벤트에서 호출
    void createPaymentFromOrder(UUID orderId, UUID userId, Integer totalAmountd);
    void cancelPaymentFromOrder(UUID orderId, String reason);

}




