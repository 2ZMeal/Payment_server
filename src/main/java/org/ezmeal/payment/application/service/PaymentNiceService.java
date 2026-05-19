package org.ezmeal.payment.application.service;

import com.ezmeal.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ezmeal.payment.application.dto.request.NicePayConfirmRequest;
import org.ezmeal.payment.application.dto.response.NicePayApprovalResponse;
import org.ezmeal.payment.application.dto.response.PaymentResponseDto;
import org.ezmeal.payment.domain.event.PaymentEventProducer;
import org.ezmeal.payment.domain.exception.PaymentErrorCode;
import org.ezmeal.payment.domain.model.Payment;
import org.ezmeal.payment.domain.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Nice Pay 결제 서비스
 *
 * 책임:
 * 1. Nice Pay 결제 승인 처리
 * 2. 서명(signature) 검증
 * 3. Nice Pay API 호출
 * 4. DB 업데이트
 * 5. Kafka 이벤트 발행
 *
 * 흐름:
 * 1. approveNicePayment() 호출
 * 2. 서명 검증 (위변조 확인)
 * 3. 주문 조회 및 금액 검증
 * 4. Nice Pay API 호출 (/v1/payments/{tid})
 * 5. DB에 결제 정보 저장
 * 6. Kafka로 결제 승인 이벤트 발행
 * 7. Order Service가 구독하여 주문 상태 업데이트
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentNiceService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final RestTemplate restTemplate;  // ← 이 줄 추가

    @Value("${payment.nicepay.client-key}")
    private String clientKey;

    @Value("${payment.nicepay.secret-key}")
    private String secretKey;

    @Value("${payment.nicepay.sandbox}")
    private boolean isSandbox;

    @Value("${payment.nicepay.sandbox-url}")
    private String sandboxUrl;

    @Value("${payment.nicepay.api-url}")
    private String apiUrl;

    /**
     * Nice Pay 결제 승인 처리
     *
     * @param request Nice Pay 인증 완료 후 returnUrl로 받은 데이터
     * @param currentUserId 현재 사용자 ID
     * @return 결제 완료 정보
     * @throws CustomException 검증 실패시
     */
    @Transactional
    public PaymentResponseDto approveNicePayment(
            NicePayConfirmRequest request,
            UUID currentUserId) {

        log.info("[Nice Pay] 결제 승인 시작 - tid: {}, orderId: {}, amount: {}",
                request.getTid(), request.getOrderId(), request.getAmount());

        Payment payment = null;  // ← 여기서 선언!

        try {
            // 1. 인증 성공 여부 확인
            if (!request.isAuthSuccess()) {
                log.warn("[Nice Pay] 인증 실패 - authResultCode: {}, msg: {}",
                        request.getAuthResultCode(), request.getAuthResultMsg());
                throw new CustomException(PaymentErrorCode.PAYMENT_AUTHENTICATION_FAILED);
            }

            // 2. 서명 검증 (위변조 확인)
            validateNicePaySignature(request);
            log.debug("[Nice Pay] 서명 검증 완료");

            // 3. 주문 조회
            UUID orderId = UUID.fromString(request.getOrderId());
            payment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> {
                        log.error("[Nice Pay] 주문을 찾을 수 없음 - orderId: {}", orderId);
                        return new CustomException(PaymentErrorCode.PAYMENT_NOT_FOUND);
                    });

            // 4. 사용자 검증
            if (!payment.getUserId().equals(currentUserId)) {
                log.warn("[Nice Pay] 사용자 불일치 - paymentUserId: {}, currentUserId: {}",
                        payment.getUserId(), currentUserId);
                throw new CustomException(PaymentErrorCode.ACCESS_DENIED);
            }

            // 5. 금액 검증 (위변조 확인)
            if (!payment.getTotalPrice().equals(request.getAmount())) {
                log.warn("[Nice Pay] 금액 불일치 - expected: {}, actual: {}",
                        payment.getTotalPrice(), request.getAmount());
                throw new CustomException(PaymentErrorCode.INVALID_PAYMENT_AMOUNT);
            }

            // 6. Nice Pay API 호출 (최종 승인)
            NicePayApprovalResponse approvalResponse = callNicePayApprovalApi(
                    request.getTid(),
                    request.getAmount()
            );

            // 7. API 응답 검증
            if (!"0000".equals(approvalResponse.getResultCode())) {
                log.error("[Nice Pay] API 승인 실패 - resultCode: {}, resultMsg: {}",
                        approvalResponse.getResultCode(),
                        approvalResponse.getResultMsg());
                throw new CustomException(PaymentErrorCode.PAYMENT_APPROVAL_FAILED);
            }

            // 8. DB 업데이트 (결제 완료)
            payment.complete(approvalResponse.getTid());
            Payment savedPayment = paymentRepository.save(payment);
            log.info("[Nice Pay] 결제 완료 - paymentId: {}, tid: {}",
                    savedPayment.getPaymentId(), approvalResponse.getTid());

            // 9. Kafka 이벤트 발행 (Order Service 구독)
            org.ezmeal.payment.domain.event.payload.PaymentCompletedEvent event =
                    org.ezmeal.payment.domain.event.payload.PaymentCompletedEvent.of(
                            savedPayment.getPaymentId(),
                            savedPayment.getOrderId(),
                            savedPayment.getUserId(),
                            savedPayment.getTotalPrice(),
                            savedPayment.getPgTransactionId()
                    );
            paymentEventProducer.publishCompletedEvent(event);

            // 10. 응답 반환
            return PaymentResponseDto.from(savedPayment);

        } catch (CustomException e) {
            log.error("[Nice Pay] 결제 승인 실패 - {}", e.getMessage());

            // ✅ 결제 실패 처리
            handlePaymentFailure(payment, e.getMessage(), "CUSTOM_ERROR");

            throw e;

        } catch (Exception e) {
            log.error("[Nice Pay] 예상치 못한 에러 발생", e);

            // ✅ 결제 실패 처리
            handlePaymentFailure(payment, e.getMessage(), "UNKNOWN_ERROR");

            throw new CustomException(PaymentErrorCode.PAYMENT_APPROVAL_FAILED);
        }
    }

    /**
     * Nice Pay 서명(Signature) 검증
     *
     * 규칙: hex(sha256(authToken + clientId + amount + secretKey))
     * 프론트에서 생성한 서명과 백엔드에서 생성한 서명이 일치하는지 확인
     *
     * @param request 요청 데이터
     * @throws CustomException 서명이 일치하지 않을 경우
     */
    private void validateNicePaySignature(NicePayConfirmRequest request) {
        try {
            String expectedSignature = generateSignature(
                    request.getAuthToken(),
                    clientKey,
                    request.getAmount(),
                    secretKey
            );

            if (!expectedSignature.equals(request.getSignature())) {
                log.warn("[Nice Pay] 서명 불일치 - expected: {}, actual: {}",
                        expectedSignature, request.getSignature());
                throw new CustomException(PaymentErrorCode.INVALID_SIGNATURE);
            }

            log.debug("[Nice Pay] 서명 검증 성공");

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Nice Pay] 서명 검증 중 에러", e);
            throw new CustomException(PaymentErrorCode.INVALID_SIGNATURE);
        }
    }

    /**
     * SHA-256 기반 서명 생성
     *
     * 규칙: hex(sha256(authToken + clientId + amount + secretKey))
     *
     * @param authToken Nice Pay에서 반환한 인증 토큰
     * @param clientId 클라이언트 ID
     * @param amount 결제 금액
     * @param secretKey 시크릿 키
     * @return 생성된 서명 (16진수 문자열)
     */
    private String generateSignature(String authToken, String clientId,
                                     Integer amount, String secretKey) {
        try {
            // 서명 데이터 구성
            String data = authToken + clientId + amount + secretKey;

            // SHA-256 해시 계산
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));

            // 바이트를 16진수 문자열로 변환
            return bytesToHex(hash);

        } catch (NoSuchAlgorithmException e) {
            log.error("[Nice Pay] SHA-256 알고리즘을 찾을 수 없음", e);
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * 바이트 배열을 16진수 문자열로 변환
     *
     * @param bytes 바이트 배열
     * @return 16진수 문자열
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Nice Pay 결제 승인 API 호출
     *
     * 엔드포인트: POST /v1/payments/{tid}
     * 요청 본문: { "amount": 1004 }
     *
     * @param tid Nice Pay 거래 ID
     * @param amount 결제 금액
     * @return Nice Pay API 응답
     * @throws CustomException API 호출 실패시
     */
    private NicePayApprovalResponse callNicePayApprovalApi(String tid, Integer amount) {
        String url = getNicePayApiUrl() + "/" + tid;

        try {
            log.debug("[Nice Pay] API 호출 시작 - URL: {}, tid: {}, amount: {}",
                    url, tid, amount);

            // Basic Auth 헤더 생성
            String credentials = clientKey + ":" + secretKey;
            String encodedCredentials = Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            // HTTP 헤더 구성
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + encodedCredentials);

            // 요청 본문
            Map<String, Integer> body = Map.of("amount", amount);
            HttpEntity<Map<String, Integer>> entity = new HttpEntity<>(body, headers);

            // API 호출
            ResponseEntity<NicePayApprovalResponse> response = restTemplate.postForEntity(
                    url,
                    entity,
                    NicePayApprovalResponse.class
            );

            NicePayApprovalResponse approvalResponse = response.getBody();

            log.info("[Nice Pay] API 호출 성공 - resultCode: {}, tid: {}",
                    approvalResponse.getResultCode(),
                    approvalResponse.getTid());

            return approvalResponse;

        } catch (HttpClientErrorException e) {
            log.error("[Nice Pay] API 호출 클라이언트 에러 - statusCode: {}, body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(PaymentErrorCode.PAYMENT_APPROVAL_FAILED);

        } catch (HttpServerErrorException e) {
            log.error("[Nice Pay] API 호출 서버 에러 - statusCode: {}, body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(PaymentErrorCode.PAYMENT_APPROVAL_FAILED);

        } catch (Exception e) {
            log.error("[Nice Pay] API 호출 중 예상치 못한 에러", e);
            throw new CustomException(PaymentErrorCode.PAYMENT_APPROVAL_FAILED);
        }
    }

    /**
     * Nice Pay API URL 결정 (샌드박스 or 운영)
     *
     * @return API URL
     */
    private String getNicePayApiUrl() {
        return isSandbox ? sandboxUrl : apiUrl;
    }

    /**
     * 결제 실패 처리
     * - Payment 상태를 FAILED로 업데이트
     * - Kafka에 결제 실패 이벤트 발행
     *
     * @param payment Payment 엔티티
     * @param failureReason 실패 사유
     * @param errorCode 에러 코드
     */
    private void handlePaymentFailure(Payment payment, String failureReason, String errorCode) {
        try {
            log.info("[Nice Pay] 결제 실패 처리: paymentId={}, reason={}",
                    payment.getPaymentId(), failureReason);

            // 1. Payment 상태 업데이트 (FAILED)
            // TODO: Payment 엔티티에 fail() 메서드가 있으면 사용
            // payment.fail(failureReason);
            // 또는 직접 상태 변경
            payment.updateStatus(
                    org.ezmeal.payment.domain.model.enums.PaymentStatus.FAILED,
                    null
            );

            Payment savedPayment = paymentRepository.save(payment);
            log.info("[Nice Pay] Payment 상태 업데이트 완료: paymentId={}, status={}",
                    savedPayment.getPaymentId(), savedPayment.getStatus());

            // 2. Kafka 이벤트 발행
            org.ezmeal.payment.domain.event.payload.PaymentFailedEvent failedEvent =
                    org.ezmeal.payment.domain.event.payload.PaymentFailedEvent.of(
                            savedPayment.getPaymentId(),
                            savedPayment.getOrderId(),
                            savedPayment.getUserId(),
                            savedPayment.getTotalPrice(),
                            failureReason,
                            errorCode
                    );

            paymentEventProducer.publishFailedEvent(failedEvent);
            log.info("[Nice Pay] 결제 실패 이벤트 발행 완료: paymentId={}",
                    savedPayment.getPaymentId());

        } catch (Exception e) {
            log.error("[Nice Pay] 결제 실패 처리 중 에러: paymentId={}",
                    payment.getPaymentId(), e);
            // 실패 처리 실패는 로그만 남기고 계속 진행
        }
    }




}
