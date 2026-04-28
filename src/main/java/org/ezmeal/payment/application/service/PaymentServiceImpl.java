package org.ezmeal.payment.application.service;

import lombok.RequiredArgsConstructor;
import org.ezmeal.payment.application.dto.request.PaymentRequestDto;
import org.ezmeal.payment.application.dto.response.PaymentResponseDto;
import org.ezmeal.payment.domain.model.Payment;
import org.ezmeal.payment.domain.model.PaymentLog;
import org.ezmeal.payment.domain.model.enums.*; // Enum 패키지 위치에 맞게 수정
import org.ezmeal.payment.domain.repository.PaymentLogRepository;
import org.ezmeal.payment.domain.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentLogRepository paymentLogRepository;

    @Override
    @Transactional
    public PaymentResponseDto createPayment(PaymentRequestDto requestDto) {
        // 1. 결제 엔티티 생성
        Payment payment = Payment.builder()
                .orderId(requestDto.getOrderId())
                .userId(requestDto.getUserId())
                .status(PaymentStatus.READY)
                .price(requestDto.getPrice())
                .totalPrice(requestDto.getTotalPrice())
                .pgProvider(requestDto.getPgProvider())
                .paymentMethod(requestDto.getPaymentMethod())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // 2. 결제 로그 기록 (REQUEST 타입)
        PaymentLog log = PaymentLog.builder()
                .payment(savedPayment)
                .paymentMethod(savedPayment.getPaymentMethod())
                .logType(LogType.REQUEST)
                .status(PaymentStatus.READY)
                .requestData(requestDto.toString()) // 실제로는 JSON 직렬화 권장
                .build();

        paymentLogRepository.save(log);

        return PaymentResponseDto.from(savedPayment);
    }

    @Override
    @Transactional
    public PaymentResponseDto approvePayment(String paymentKey, String orderId, Integer amount) {
        // TODO: Infrastructure 레이어의 Toss Client를 호출하여 PG 승인 진행
        // TODO: 승인 결과에 따라 결제 상태 업데이트 및 로그 저장

        throw new UnsupportedOperationException("approvePayment is not yet implemented");
    }
}
