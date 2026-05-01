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

    @Value("${payment.toss.secret-key}")
    private String secretKey;

    @Override
    @Transactional
    public PaymentResponseDto createPayment(PaymentRequestDto requestDto,  UUID authenticatedUserId) {

        // 1. 결제 엔티티 생성
        Payment payment = Payment.builder()
                .orderId(requestDto.getOrderId())
                .userId(authenticatedUserId) // 컨트롤러가 헤더에서 ID를 여기에 세팅
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
    public PaymentResponseDto approvePayment(TossConfirmRequest request, UUID currentUserId) {
        // 1. DB 조회
        Payment payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(request.getOrderId())
                .orElseThrow(() -> new CustomException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        // [검증 1] 중복 승인 방지 (이미 완료/실패된 건인지 확인)
        if (payment.getStatus() != PaymentStatus.READY) {
            throw new CustomException(PaymentErrorCode.ALREADY_PROCESSED_PAYMENT);
        }

        // [검증 2] 보안: 결제 요청자와 현재 로그인 유저가 일치하는지 확인
        if (!payment.getUserId().equals(currentUserId)) {
            throw new CustomException(PaymentErrorCode.ACCESS_DENIED);
        }

        // [검증 3] 금액 검증 (DB 저장 금액 vs 클라이언트 요청 금액)
        //  여기서 요청 금액(request.getAmount())이 DB랑 다르면 바로 컷
        if (!payment.getPrice().equals(request.getAmount())) {
            throw new CustomException(PaymentErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        try {
            // 2. 토스 API 인증 헤더 생성
            String auth = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());

            // 3. PG사 승인 요청
            TossConfirmResponse response = tossPaymentClient.confirmPayment("Basic " + auth, request);

            // [검증 4] paymentKey 대조 (토스 응답 키 vs 요청 키)
            // 🚩 요청한 키와 응답받은 키가 다르면 데이터 변조로 간주합니다.
            if (!response.getPaymentKey().equals(request.getPaymentKey())) {
                throw new CustomException(PaymentErrorCode.INVALID_PAYMENT_KEY);
            }

            // [검증 5] 최종 금액 대조 (토스 응답 실제 결제 금액 vs DB 저장 금액)
            // 🚩 실제 돈이 얼마 나갔는지 토스가 알려준 값(totalAmount)까지 확인해야 끝입니다.
            if (response.getTotalAmount() == null
                    || payment.getPrice().longValue() != response.getTotalAmount()) {
                throw new CustomException(PaymentErrorCode.INVALID_PAYMENT_AMOUNT);
            }

            // 4. 모든 검증 통과 시 상태 업데이트 (paymentKey 저장)
            payment.updateStatus(PaymentStatus.COMPLETED, response.getPaymentKey());

            // 성공 로그 기록
            paymentLogRepository.save(PaymentLog.builder()
                    .payment(payment)
                    .logType(LogType.APPROVE)
                    .status(PaymentStatus.COMPLETED)
                    .requestData(request.toString())
                    .responseData(response.toString())
                    .build());

            return PaymentResponseDto.from(payment);

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            // 5. 실패 시 처리
            payment.updateStatus(PaymentStatus.FAILED, request.getPaymentKey());

            paymentLogRepository.save(PaymentLog.builder()
                    .payment(payment)
                    .logType(LogType.APPROVE)
                    .status(PaymentStatus.FAILED)
                    .requestData(e.getMessage())
                    .build());

            throw new CustomException(PaymentErrorCode.PAYMENT_GATEWAY_ERROR);
        }
    }



}
