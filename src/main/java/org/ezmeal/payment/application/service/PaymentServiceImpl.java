package org.ezmeal.payment.application.service;

import com.ezmeal.common.exception.CustomException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ezmeal.payment.application.dto.request.PaymentRequestDto;
import org.ezmeal.payment.application.dto.response.PaymentResponseDto;
import org.ezmeal.payment.domain.exception.PaymentErrorCode;
import org.ezmeal.payment.domain.model.Payment;
import org.ezmeal.payment.domain.model.PaymentLog;
import org.ezmeal.payment.domain.model.enums.LogType;
import org.ezmeal.payment.domain.model.enums.PaymentStatus;
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

        /*
        // [추가] 중복 결제 요청 검증 컨트롤러 만들고 수정해보기
        paymentRepository.findByOrderId(requestDto.getOrderId()).ifPresent(p -> {
            throw new CustomException(PaymentErrorCode.ALREADY_PROCESSED_PAYMENT);
        });*/


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
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentDetail(UUID paymentId) {
        // 1. DB에서 결제 정보 조회 (없으면 라이브러리의 CustomException 던지기)
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        // 2. DTO로 변환하여 반환
        return PaymentResponseDto.from(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getPaymentList() {
        // 1. 모든 결제 내역 조회 (실제로는 페이징 처리가 필요하지만 일단 리스트로 구현)
        List<Payment> payments = paymentRepository.findAll();

        // 2. 리스트 변환 (Stream 활용)
        return payments.stream()
                .map(PaymentResponseDto::from)
                .collect(Collectors.toList());
    }





    @Override
    @Transactional
    public PaymentResponseDto approvePayment(String paymentKey, String orderId, Integer amount) {
        // TODO: Infrastructure 레이어의 Toss Client를 호출하여 PG 승인 진행
        // TODO: 승인 결과에 따라 결제 상태 업데이트 및 로그 저장

        throw new UnsupportedOperationException("approvePayment is not yet implemented");
    }



}
