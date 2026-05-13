package org.ezmeal.payment.presentation.controller;

import com.ezmeal.common.exception.CustomException;
import com.ezmeal.common.response.CommonApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ezmeal.payment.application.dto.request.NicePayConfirmRequest;
import org.ezmeal.payment.application.dto.request.TossConfirmRequest;
import org.ezmeal.payment.application.dto.response.PaymentResponseDto;
import org.ezmeal.payment.application.service.PaymentNiceService;
import org.ezmeal.payment.application.service.PaymentService;
import org.ezmeal.payment.domain.exception.PaymentErrorCode;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * PG 제공사별 결제 승인 API Controller
 *
 * 책임:
 * 1. Toss Payments 결제 승인
 * 2. Nice Pay 결제 승인
 * 3. (향후) Kakao Pay, Stripe 등 추가
 *
 * 설계:
 * - /api/v1/pg/{pgname}/payments/confirm 형식
 * - 각 PG사별 서비스 주입
 * - PG사별 요청/응답 DTO 구분
 *
 * 엔드포인트:
 * - POST /api/v1/pg/toss/payments/confirm (Toss 승인)
 * - POST /api/v1/pg/nicepay/payments/confirm (Nice Pay 승인)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/pg")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4000", "http://localhost:5173"}, allowedHeaders = "*")
public class PgController {

    private final PaymentService paymentService;           // Toss
    private final PaymentNiceService paymentNiceService;   // Nice Pay

    /**
     * Toss Payments 결제 승인
     *
     * 프론트에서 Toss 결제창 인증 후 호출
     * - 서명 검증
     * - Toss API 호출 (최종 승인)
     * - DB 업데이트
     * - Kafka 이벤트 발행
     *
     * 요청 본문 예시:
     * {
     *   "paymentKey": "toss_key_123...",
     *   "orderId": "order-123",
     *   "amount": 10000
     * }
     *
     * @param requestDto Toss 인증 응답 데이터
     * @param userIdHeader 사용자 ID (헤더)
     * @return 결제 완료 정보
     */
    @PostMapping("/toss/payments/confirm")
    public CommonApiResponse<PaymentResponseDto> confirmTossPayment(
            @Valid @RequestBody TossConfirmRequest requestDto,
            @RequestHeader(value = "X-User-Id") String userIdHeader
    ) {
        UUID currentUserId = parseUserId(userIdHeader);

        log.info("[Toss] 결제 승인 시작 - paymentKey: {}, amount: {}",
                requestDto.getPaymentKey(), requestDto.getAmount());

        try {
            PaymentResponseDto response = paymentService.approvePayment(requestDto, currentUserId);

            log.info("[Toss] 결제 승인 완료 - paymentId: {}", response.getPaymentId());

            return CommonApiResponse.success("Toss 결제 승인 성공", response);

        } catch (CustomException e) {
            log.error("[Toss] 결제 승인 실패 - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[Toss] 예상치 못한 에러", e);
            throw new CustomException(PaymentErrorCode.PAYMENT_APPROVAL_FAILED);
        }
    }

    /**
     * Nice Pay 결제 승인
     *
     * 프론트에서 Nice Pay 결제창 인증 후 호출
     *
     * 흐름:
     * 1. 프론트: AUTHNICE.requestPay() 호출
     * 2. 사용자 인증 완료
     * 3. returnUrl로 POST 요청 (NicePayConfirmRequest 포함)
     * 4. 이 엔드포인트 호출
     * 5. 서명 검증 + Nice Pay API 호출
     * 6. DB 업데이트 + Kafka 이벤트
     *
     * 요청 본문 예시:
     * {
     *   "authResultCode": "0000",
     *   "tid": "UT0000113m01012110051656331001",
     *   "orderId": "order-123",
     *   "amount": 10000,
     *   "authToken": "NICEUNTT9FBBD87FD...",
     *   "signature": "7cc95c592e2a12f0..."
     * }
     *
     * @param requestDto Nice Pay 인증 응답 데이터
     * @param userIdHeader 사용자 ID (헤더)
     * @return 결제 완료 정보
     */
    @PostMapping("/nicepay/payments/confirm")
    public CommonApiResponse<PaymentResponseDto> confirmNicePayPayment(
            @Valid @RequestBody NicePayConfirmRequest requestDto,
            @RequestHeader(value = "X-User-Id") String userIdHeader
    ) {
        UUID currentUserId = parseUserId(userIdHeader);

        log.info("[NicePay] 결제 승인 시작 - tid: {}, orderId: {}, amount: {}",
                requestDto.getTid(), requestDto.getOrderId(), requestDto.getAmount());

        try {
            PaymentResponseDto response = paymentNiceService.approveNicePayment(requestDto, currentUserId);

            log.info("[NicePay] 결제 승인 완료 - paymentId: {}", response.getPaymentId());

            return CommonApiResponse.success("Nice Pay 결제 승인 성공", response);

        } catch (CustomException e) {
            log.error("[NicePay] 결제 승인 실패 - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[NicePay] 예상치 못한 에러", e);
            throw new CustomException(PaymentErrorCode.PAYMENT_APPROVAL_FAILED);
        }
    }

    /**
     * User ID 파싱 헬퍼 메서드
     *
     * 헤더에서 받은 userId를 UUID로 변환
     * 잘못된 형식은 예외 발생
     *
     * @param userIdHeader 헤더의 X-User-Id 값
     * @return 파싱된 UUID
     * @throws CustomException UUID 파싱 실패시
     */
    private UUID parseUserId(String userIdHeader) {
        try {
            return UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warn("[Pg] userId 파싱 실패 - header: {}", userIdHeader);
            throw new CustomException(PaymentErrorCode.INVALID_USER_ID);
        }
    }
}
