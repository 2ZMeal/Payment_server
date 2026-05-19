package org.ezmeal.payment.application.service;

import com.ezmeal.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ezmeal.payment.application.dto.request.PaymentRequestDto;
import org.ezmeal.payment.application.dto.request.TossConfirmRequest;
import org.ezmeal.payment.application.dto.response.PaymentResponseDto;
import org.ezmeal.payment.application.dto.response.TossConfirmResponse;
import org.ezmeal.payment.domain.event.PaymentEventProducer;
import org.ezmeal.payment.domain.event.payload.PaymentCancelledEvent;
import org.ezmeal.payment.domain.event.payload.PaymentCompletedEvent;
import org.ezmeal.payment.domain.event.payload.PaymentFailedEvent;
import org.ezmeal.payment.domain.exception.PaymentErrorCode;
import org.ezmeal.payment.domain.model.Payment;
import org.ezmeal.payment.domain.model.PaymentLog;
import org.ezmeal.payment.domain.model.enums.LogType;
import org.ezmeal.payment.domain.model.enums.PaymentMethod;
import org.ezmeal.payment.domain.model.enums.PaymentStatus;
import org.ezmeal.payment.domain.model.enums.PgProvider;
import org.ezmeal.payment.domain.repository.PaymentLogRepository;
import org.ezmeal.payment.domain.repository.PaymentRepository;
import org.ezmeal.payment.infrastructure.client.TossPaymentClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient.Builder;

import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final TossPaymentClient tossPaymentClient;
    private final PaymentEventProducer paymentEventProducer;
    private final Builder builder;

    @Value("${payment.toss.secret-key}")
    private String secretKey;

    @Override
    @Transactional
    public PaymentResponseDto createPayment(PaymentRequestDto requestDto,  UUID authenticatedUserId) {

        // 🎯 결제 요청 시작 로그
        log.info("[결제 생성 요청] orderId: {}, userId: {}, amount: {}", requestDto.getOrderId(), authenticatedUserId, requestDto.getTotalPrice());

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
        PaymentLog paymentLogEntity  = PaymentLog.builder()
                .payment(savedPayment)
                .paymentMethod(savedPayment.getPaymentMethod())
                .logType(LogType.REQUEST)
                .status(PaymentStatus.READY)
                .requestData(requestDto.toString()) // 실제로는 JSON 직렬화 권장
                .build();

        paymentLogRepository.save(paymentLogEntity );

        // 🎯 결제 생성 완료 로그
        log.info("[결제 생성 완료] paymentId: {}, DB 저장 성공", savedPayment.getPaymentId());

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
    public PaymentResponseDto getPaymentByOrderId(UUID orderId) {
        // orderId로 가장 최근의 결제 정보 조회
        Payment payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new CustomException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        // DTO로 변환하여 반환
        return PaymentResponseDto.from(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getPaymentList(UUID currentUserId, String roles) {
        // 1. 모든 결제 내역 조회 (실제로는 페이징 처리가 필요하지만 일단 리스트로 구현)
        List<Payment> payments = hasRole(roles, "ADMIN")
                ? paymentRepository.findAll()
                : paymentRepository.findAllByUserId(currentUserId);

        // 2. 리스트 변환 (Stream 활용)
        return payments.stream()
                .map(PaymentResponseDto::from)
                .collect(Collectors.toList());
    }

    private boolean hasRole(String roles, String role) {
        if (roles == null || roles.isBlank()) {
            return false;
        }

        return List.of(roles.split(",")).stream()
                .map(String::trim)
                .anyMatch(value -> value.equals(role) || value.equals("ROLE_" + role));
    }


    @Override
    @Transactional
    public PaymentResponseDto approvePayment(TossConfirmRequest request, UUID currentUserId) {

        log.info("[결제 승인 요청 수신] orderId: {}, paymentKey: {}, amount: {}", request.getOrderId(), request.getPaymentKey(), request.getAmount());

        // 1. DB 조회
        Payment payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(request.getOrderId())
                .orElseThrow(() -> new CustomException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        // [검증 1] 중복 승인 방지
        if (payment.getStatus() != PaymentStatus.READY) {
            paymentEventProducer.publishFailedEvent(PaymentFailedEvent.of(
                    payment.getPaymentId(), payment.getOrderId(), payment.getUserId(),
                    payment.getPrice(), "이미 처리된 결제입니다.", "ALREADY_PROCESSED"
            ));
            throw new CustomException(PaymentErrorCode.ALREADY_PROCESSED_PAYMENT);
        }

        // [검증 2] 결제 요청자 확인
        if (!payment.getUserId().equals(currentUserId)) {
            paymentEventProducer.publishFailedEvent(PaymentFailedEvent.of(
                    payment.getPaymentId(), payment.getOrderId(), payment.getUserId(),
                    payment.getPrice(), "결제 요청자가 일치하지 않습니다.", "ACCESS_DENIED"
            ));
            throw new CustomException(PaymentErrorCode.ACCESS_DENIED);
        }

        // [검증 3] 금액 검증
        if (!payment.getPrice().equals(request.getAmount())) {
            paymentEventProducer.publishFailedEvent(PaymentFailedEvent.of(
                    payment.getPaymentId(), payment.getOrderId(), payment.getUserId(),
                    payment.getPrice(), "요청 금액이 일치하지 않습니다.", "INVALID_AMOUNT"
            ));
            throw new CustomException(PaymentErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        if (payment.getPaymentMethod() == PaymentMethod.CARD) {
            String cardTransactionId = "CARD-" + UUID.randomUUID();
            payment.updateStatus(PaymentStatus.COMPLETED, cardTransactionId);

            paymentLogRepository.save(PaymentLog.builder()
                    .payment(payment)
                    .paymentMethod(resolvePaymentMethod(payment))
                    .logType(LogType.APPROVE)
                    .status(PaymentStatus.COMPLETED)
                    .requestData(request.toString())
                    .responseData(cardTransactionId)
                    .build());

            publishCompletedEventSafely(PaymentCompletedEvent.of(
                    payment.getPaymentId(), payment.getOrderId(), payment.getUserId(),
                    payment.getPrice(), cardTransactionId
            ));

            return PaymentResponseDto.from(payment);
        }

        try {
            String auth = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());
            TossConfirmResponse response = tossPaymentClient.confirmPayment("Basic " + auth, request);

            // [검증 4] paymentKey 대조
            if (!response.getPaymentKey().equals(request.getPaymentKey())) {
                paymentEventProducer.publishFailedEvent(PaymentFailedEvent.of(
                        payment.getPaymentId(), payment.getOrderId(), payment.getUserId(),
                        payment.getPrice(), "결제 키가 일치하지 않습니다!!.", "INVALID_PAYMENT_KEY"
                ));
                throw new CustomException(PaymentErrorCode.INVALID_PAYMENT_KEY);
            }

            // [검증 5] 최종 금액 대조
            if (response.getTotalAmount() == null
                    || payment.getPrice().longValue() != response.getTotalAmount()) {
                paymentEventProducer.publishFailedEvent(PaymentFailedEvent.of(
                        payment.getPaymentId(), payment.getOrderId(), payment.getUserId(),
                        payment.getPrice(), "최종 결제 금액이 일치하지 않습니다.", "INVALID_AMOUNT"
                ));
                throw new CustomException(PaymentErrorCode.INVALID_PAYMENT_AMOUNT);
            }

            payment.updateStatus(PaymentStatus.COMPLETED, response.getPaymentKey());

            paymentLogRepository.save(PaymentLog.builder()
                    .payment(payment)
                    .paymentMethod(resolvePaymentMethod(payment))
                    .logType(LogType.APPROVE)
                    .status(PaymentStatus.COMPLETED)
                    .requestData(request.toString())
                    .responseData(response.toString())
                    .build());

            log.info("[결제 승인 완료] 토스 최종 승인 및 DB 반영 성공 - paymentKey: {}", response.getPaymentKey());

            publishCompletedEventSafely(PaymentCompletedEvent.of(
                    payment.getPaymentId(), payment.getOrderId(), payment.getUserId(),
                    payment.getPrice(), response.getPaymentKey()
            ));

            return PaymentResponseDto.from(payment);

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            payment.updateStatus(PaymentStatus.FAILED, request.getPaymentKey());

            paymentLogRepository.save(PaymentLog.builder()
                    .payment(payment)
                    .paymentMethod(resolvePaymentMethod(payment))
                    .logType(LogType.APPROVE)
                    .status(PaymentStatus.FAILED)
                    .requestData(e.getMessage())
                    .build());

            // ✅ 추가!
            paymentEventProducer.publishFailedEvent(PaymentFailedEvent.of(
                    payment.getPaymentId(), payment.getOrderId(), payment.getUserId(),
                    payment.getPrice(), e.getMessage(), "PAYMENT_GATEWAY_ERROR"
            ));

            throw new CustomException(PaymentErrorCode.PAYMENT_GATEWAY_ERROR);
        }
    }

    @Override
    @Transactional
    public PaymentResponseDto cancelPayment(UUID paymentId, String cancelReason, UUID currentUserId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.getUserId().equals(currentUserId)) {
            throw new CustomException(PaymentErrorCode.ACCESS_DENIED);
        }

        if (payment.getStatus() == PaymentStatus.CANCELLED
                || payment.getStatus() == PaymentStatus.FAILED) {
            throw new CustomException(PaymentErrorCode.ALREADY_PROCESSED_PAYMENT);
        }

        payment.cancel(cancelReason);


        paymentLogRepository.save(PaymentLog.builder()
                .payment(payment)
                .paymentMethod(payment.getPaymentMethod())
                .logType(LogType.CANCEL)
                .status(PaymentStatus.CANCELLED)
                .requestData(cancelReason)
                .build());


        // kafka 이벤트
        PaymentCancelledEvent event = PaymentCancelledEvent.of(
                payment.getPaymentId(),      // ✅ 실제 객체에서 가져옴
                payment.getOrderId(),
                payment.getUserId(),
                payment.getPrice(),
                cancelReason
        );

        paymentEventProducer.publishCancelledEvent(event);


        return PaymentResponseDto.from(payment);

    }


    //kafka
    @Override
    public void completePayment(UUID paymentId, String paymentKey, UUID currentUserId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        payment.complete(paymentKey);
        paymentRepository.save(payment);

        log.info("결제 완료: paymentId={}, userId={}", paymentId, currentUserId);
    }
    /**
     * Order Service의 주문 생성 이벤트에서 호출
     * - Payment 엔티티 생성
     * - status: READY
     *
     * @param orderId 주문 ID
     * @param userId 사용자 ID
     * @param totalAmount 총 결제 금액
     */
    @Override
    @Transactional
    public void createPaymentFromOrder(UUID orderId, UUID userId, Integer totalAmount) {
        log.info("[Payment] 주문 기반 결제 생성: orderId={}, userId={}, amount={}",
                orderId, userId, totalAmount);

        try {
            // 기존 Payment 있는지 확인
            if (paymentRepository.findByOrderId(orderId).isPresent()) {
                log.warn("[Payment] 이미 존재하는 Payment: orderId={}", orderId);
                return;
            }

            // Payment 엔티티 생성
            Payment payment = Payment.builder()
                    .orderId(orderId)
                    .userId(userId)
                    .status(PaymentStatus.READY)
                    .price(totalAmount)
                    .totalPrice(totalAmount)
                    .pgProvider(PgProvider.TOSS)
                    .paymentMethod(PaymentMethod.TOSS)
                    .build();

            Payment savedPayment = paymentRepository.save(payment);
            log.info("[Payment] 결제 생성 완료: paymentId={}, orderId={}",
                    savedPayment.getPaymentId(), orderId);

        } catch (Exception e) {
            log.error("[Payment] 주문 기반 결제 생성 실패: orderId={}", orderId, e);
            throw new RuntimeException("결제 생성 실패", e);
        }
    }

    /**
     * Order Service의 주문 취소 이벤트에서 호출
     * - Payment 상태 변경: CANCELLED
     *
     * @param orderId 주문 ID
     * @param reason 취소 사유
     */
    @Override
    @Transactional
    public void cancelPaymentFromOrder(UUID orderId, String reason) {
        log.info("[Payment] 주문 기반 결제 취소: orderId={}, reason={}", orderId, reason);

        try {
            Payment payment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> {
                        log.warn("[Payment] Payment를 찾을 수 없음: orderId={}", orderId);
                        return new CustomException(PaymentErrorCode.PAYMENT_NOT_FOUND);
                    });

            // 이미 취소된 경우
            if (payment.getStatus() == PaymentStatus.CANCELLED) {
                log.warn("[Payment] 이미 취소된 Payment: orderId={}", orderId);
                return;
            }

            // Payment 취소
            payment.cancel(reason);
            Payment savedPayment = paymentRepository.save(payment);

            // ✅ 이벤트 발행 추가!
            PaymentCancelledEvent event = PaymentCancelledEvent.of(
                    savedPayment.getPaymentId(),
                    savedPayment.getOrderId(),
                    savedPayment.getUserId(),
                    savedPayment.getPrice(),
                    reason
            );
            paymentEventProducer.publishCancelledEvent(event);

            log.info("[Payment] 결제 취소 완료: paymentId={}, orderId={}, status={}",
                    savedPayment.getPaymentId(), orderId, savedPayment.getStatus());

        } catch (CustomException e) {
            log.error("[Payment] 주문 기반 결제 취소 실패: orderId={}, message={}", orderId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[Payment] 주문 기반 결제 취소 중 예상치 못한 에러: orderId={}", orderId, e);
            throw new RuntimeException("결제 취소 실패", e);
        }
    }

    private PaymentMethod resolvePaymentMethod(Payment payment) {
        return payment.getPaymentMethod() != null ? payment.getPaymentMethod() : PaymentMethod.TOSS;
    }

    private void publishCompletedEventSafely(PaymentCompletedEvent event) {
        try {
            paymentEventProducer.publishCompletedEvent(event);
        } catch (Exception e) {
            log.error("[Payment] 결제는 완료됐지만 완료 이벤트 발행에 실패했습니다. paymentId={}, orderId={}",
                    event.getPaymentId(), event.getOrderId(), e);
        }
    }



}
