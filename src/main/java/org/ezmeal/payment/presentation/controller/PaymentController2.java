//package org.ezmeal.payment.presentation.controller;
//
//import com.ezmeal.common.exception.CustomException;
//import com.ezmeal.common.response.CommonApiResponse;
//import java.util.List;
//import java.util.UUID;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.ezmeal.payment.application.dto.request.PaymentCancelRequest;
//import org.ezmeal.payment.application.dto.request.PaymentRequestDto;
//import org.ezmeal.payment.application.dto.response.PaymentResponseDto;
//import org.ezmeal.payment.application.service.PaymentService;
//import org.ezmeal.payment.domain.exception.PaymentErrorCode;
//import org.springframework.web.bind.annotation.CrossOrigin;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestHeader;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
///**
// * 결제 공통 API Controller
// *
// * 책임:
// * 1. 결제 요청 생성
// * 2. 결제 조회 (단건, 목록)
// * 3. 결제 취소
// *
// * PG사별 승인은 PgController에서 처리
// *
// * 엔드포인트:
// * - POST /api/v1/payments (결제 요청 생성)
// * - GET /api/v1/payments/{payment_id} (결제 상세 조회)
// * - GET /api/v1/payments (결제 목록)
// * - POST /api/v1/payments/{payment_id}/cancel (결제 취소)
// */
//@Slf4j
//@RestController
//@RequestMapping("/api/v1/payments")
//@RequiredArgsConstructor
//@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*")
//public class PaymentController2 {
//
//    private final PaymentService paymentService;
//
//    /**
//     * 결제 요청 생성
//     *
//     * 주문 시 호출되는 엔드포인트
//     * - 결제 정보를 DB에 저장 (상태: PENDING)
//     * - paymentId를 프론트로 반환
//     * - 프론트는 이 paymentId로 PG사 결제창 호출
//     *
//     * @param requestDto 결제 요청 정보
//     * @param userIdHeader 사용자 ID (헤더)
//     * @return 생성된 결제 정보
//     */
//    @PostMapping
//    public CommonApiResponse<PaymentResponseDto> createPayment(
//            @RequestBody PaymentRequestDto requestDto,
//            @RequestHeader(value = "X-User-Id") String userIdHeader
//    ) {
//        UUID authenticatedUserId = parseUserId(userIdHeader);
//
//        log.info("[Payment] 결제 요청 생성 - orderId: {}, amount: {}",
//                requestDto.getOrderId(), requestDto.getAmount());
//
//        PaymentResponseDto response = paymentService.createPayment(requestDto, authenticatedUserId);
//
//        return CommonApiResponse.success("결제 요청이 성공적으로 생성되었습니다.", response);
//    }
//
//    /**
//     * 결제 상세 조회
//     *
//     * 특정 결제의 상세 정보를 조회
//     * - 권한 검증: 본인의 결제만 조회 가능
//     *
//     * @param paymentId 결제 ID
//     * @param userIdHeader 사용자 ID (헤더)
//     * @return 결제 상세 정보
//     */
//    @GetMapping("/{payment_id}")
//    public CommonApiResponse<PaymentResponseDto> getPayment(
//            @PathVariable("payment_id") UUID paymentId,
//            @RequestHeader(value = "X-User-Id") String userIdHeader
//    ) {
//        UUID currentUserId = parseUserId(userIdHeader);
//
//        log.info("[Payment] 결제 상세 조회 - paymentId: {}", paymentId);
//
//        PaymentResponseDto response = paymentService.getPaymentDetail(paymentId);
//
//        // 권한 검증
//        if (!response.getUserId().equals(currentUserId)) {
//            log.warn("[Payment] 접근 권한 없음 - paymentUserId: {}, currentUserId: {}",
//                    response.getUserId(), currentUserId);
//            throw new CustomException(PaymentErrorCode.ACCESS_DENIED);
//        }
//
//        return CommonApiResponse.success(response);
//    }
//
//    /**
//     * 결제 목록 조회
//     *
//     * 사용자의 모든 결제 목록을 조회
//     * - 일반 사용자: 자신의 결제만
//     * - 관리자: 전체 결제 (rolesHeader 확인)
//     *
//     * @param userIdHeader 사용자 ID (헤더)
//     * @param rolesHeader 사용자 역할 (헤더)
//     * @return 결제 목록
//     */
//    @GetMapping
//    public CommonApiResponse<List<PaymentResponseDto>> getPayments(
//            @RequestHeader(value = "X-User-Id") String userIdHeader,
//            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader
//    ) {
//        UUID currentUserId = parseUserId(userIdHeader);
//
//        log.info("[Payment] 결제 목록 조회 - userId: {}", currentUserId);
//
//        List<PaymentResponseDto> responses = paymentService.getPaymentList(currentUserId, rolesHeader);
//
//        return CommonApiResponse.success(responses);
//    }
//
//    /**
//     * 결제 취소
//     *
//     * 결제를 취소하고 환불 처리
//     * - 권한 검증: 본인의 결제만 취소 가능
//     * - 상태 검증: COMPLETED 상태만 취소 가능
//     * - PG사 환불 API 호출
//     * - DB 업데이트
//     * - Kafka 이벤트 발행
//     *
//     * @param paymentId 결제 ID
//     * @param request 취소 요청 (취소 사유)
//     * @param userIdHeader 사용자 ID (헤더)
//     * @return 취소된 결제 정보
//     */
//    @PostMapping("/{payment_id}/cancel")
//    public CommonApiResponse<PaymentResponseDto> cancelPayment(
//            @PathVariable("payment_id") UUID paymentId,
//            @RequestBody PaymentCancelRequest request,
//            @RequestHeader(value = "X-User-Id") String userIdHeader
//    ) {
//        UUID currentUserId = parseUserId(userIdHeader);
//
//        log.info("[Payment] 결제 취소 시작 - paymentId: {}, reason: {}",
//                paymentId, request.getCancelReason());
//
//        PaymentResponseDto response = paymentService.cancelPayment(
//                paymentId,
//                request.getCancelReason(),
//                currentUserId);
//
//        log.info("[Payment] 결제 취소 완료 - paymentId: {}", paymentId);
//
//        return CommonApiResponse.success("결제가 취소되었습니다.", response);
//    }
//
//    /**
//     * User ID 파싱 헬퍼 메서드
//     *
//     * 헤더에서 받은 userId를 UUID로 변환
//     * 잘못된 형식은 예외 발생
//     *
//     * @param userIdHeader 헤더의 X-User-Id 값
//     * @return 파싱된 UUID
//     * @throws CustomException UUID 파싱 실패시
//     */
//    private UUID parseUserId(String userIdHeader) {
//        try {
//            return UUID.fromString(userIdHeader);
//        } catch (IllegalArgumentException | NullPointerException e) {
//            log.warn("[Payment] userId 파싱 실패 - header: {}", userIdHeader);
//            throw new CustomException(PaymentErrorCode.INVALID_USER_ID);
//        }
//    }
//}
