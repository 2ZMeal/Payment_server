package org.ezmeal.payment.application.service;

import com.ezmeal.common.exception.CustomException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ezmeal.payment.application.dto.request.PaymentRequestDto;
import org.ezmeal.payment.application.dto.request.TossConfirmRequest;
import org.ezmeal.payment.application.dto.response.PaymentResponseDto;
import org.ezmeal.payment.application.dto.response.TossConfirmResponse;
import org.ezmeal.payment.domain.exception.PaymentErrorCode;
import org.ezmeal.payment.domain.model.Payment;
import org.ezmeal.payment.domain.model.PaymentLog;
import org.ezmeal.payment.domain.model.enums.LogType;
import org.ezmeal.payment.domain.model.enums.PaymentStatus;
import org.ezmeal.payment.domain.repository.PaymentLogRepository;
import org.ezmeal.payment.domain.repository.PaymentRepository;
import org.ezmeal.payment.infrastructure.client.TossPaymentClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final TossPaymentClient tossPaymentClient;



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



    @Transactional
    public PaymentResponseDto approvePayment(String paymentKey, UUID orderId, Integer amount) {
        // TODO: Infrastructure 레이어의 Toss Client를 호출하여 PG 승인 진행
        // TODO: 승인 결과에 따라 결제 상태 업데이트 및 로그 저장

        // 1. DB 조회 및 검증
        Payment payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new CustomException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.getTotalPrice().equals(amount)) {
            throw new CustomException(PaymentErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        try {
            // 2. 토스 API 호출을 위한 인증 헤더 생성 (SecretKey 뒤에 콜론(:)을 붙여 Base64 인코딩)
            String auth = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());

            // 3. 진짜 승인 요청
            TossConfirmResponse response = tossPaymentClient.confirmPayment("Basic " + auth,
                    new TossConfirmRequest(paymentKey, orderId, amount));

            // 4. 성공 시 상태 업데이트 및 로그 저장
            payment.updateStatus(PaymentStatus.COMPLETED, response.getPaymentKey());

            paymentLogRepository.save(PaymentLog.builder()
                    .payment(payment)
                    .logType(LogType.APPROVE)
                    .status(PaymentStatus.COMPLETED)
                    .build());

            return PaymentResponseDto.from(payment);

        } catch (Exception e) {
            // 5. 실패 시 처리
            payment.updateStatus(PaymentStatus.FAILED, null);
            throw new CustomException(PaymentErrorCode.PAYMENT_GATEWAY_ERROR);
        }

    }

    @Value("${payment.toss.secret-key}")
    private String secretKey;



}
