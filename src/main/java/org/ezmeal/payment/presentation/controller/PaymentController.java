package org.ezmeal.payment.presentation.controller;

import com.ezmeal.common.exception.CustomException;
import com.ezmeal.common.response.CommonApiResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.ezmeal.payment.application.dto.request.PaymentRequestDto;
import org.ezmeal.payment.application.dto.request.TossConfirmRequest;
import org.ezmeal.payment.application.dto.response.PaymentResponseDto;
import org.ezmeal.payment.application.service.PaymentService;
import org.ezmeal.payment.domain.exception.PaymentErrorCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 1. 결제 요청 생성 (POST /api/v1/payments)
     * 주문 진입 시 결제 데이터를 READY 상태로 생성합니다.
     */
    @PostMapping
    public CommonApiResponse<PaymentResponseDto> createPayment(
            @RequestBody PaymentRequestDto requestDto,
            @RequestHeader(value = "X-User-Id") String userIdHeader // 🚩 게이트웨이가 인증 후 넘겨준 헤더
    ) {
        // 1. 헤더 정보를 UUID로 변환
        UUID authenticatedUserId = UUID.fromString(userIdHeader);

        // 2. 서비스 호출 시 인증된 유저 ID를 별도로 전달
        PaymentResponseDto response = paymentService.createPayment(requestDto, authenticatedUserId);

        return CommonApiResponse.success("결제 요청이 성공적으로 생성되었습니다.", response);
    }


    /**
     * 2. 결제 승인 처리 (POST /api/v1/payments/confirm)
     * PG사(토스 등) 인증 완료 후 클라이언트가 전달한 정보로 최종 승인을 진행합니다.
     */
    @PostMapping("/confirm")
    public CommonApiResponse<PaymentResponseDto> confirmPayment(
            @RequestBody TossConfirmRequest requestDto,
            @RequestHeader(value = "X-User-Id") String userIdHeader // 🚩 게이트웨이가 넘겨준 헤더에서 가져옴
    ) {
        // 헤더로 넘어온 String을 UUID로 변환하여 서비스에 전달
        UUID currentUserId = UUID.fromString(userIdHeader);

        PaymentResponseDto response = paymentService.approvePayment(requestDto, currentUserId);
        return CommonApiResponse.success("결제 승인 성공", response);
    }

    /**
     * 3. 결제 단건 조회 (GET /api/v1/payments/{payment_id})
     */
    @GetMapping("/{payment_id}")
    public CommonApiResponse<PaymentResponseDto> getPayment(
            @PathVariable("payment_id") UUID paymentId,
            @RequestHeader(value = "X-User-Id") String userIdHeader
    ) {
        UUID currentUserId = UUID.fromString(userIdHeader);
        PaymentResponseDto response = paymentService.getPaymentDetail(paymentId);

        if (!response.getUserId().equals(currentUserId)) {
            throw new CustomException(PaymentErrorCode.ACCESS_DENIED);
        }

        return CommonApiResponse.success(response);
    }

    /**
     * 4. 전체 결제 조회 (GET /api/v1/payments)
     * 페이징 처리가 필요할 수 있습니다.
     */
    @GetMapping
    public CommonApiResponse<List<PaymentResponseDto>> getPayments() {
        // TODO: 페이징 및 권한별 필터링 로직 (관리자는 전체, 유저는 본인 것만)
        List<PaymentResponseDto> responses = paymentService.getPaymentList();
        return CommonApiResponse.success(responses);
    }

    /**
     * 5. 결제 취소 (POST /api/v1/payments/{payment_id}/cancel)
     */
    @PostMapping("/{payment_id}/cancel")
    public CommonApiResponse<PaymentResponseDto> cancelPayment(
            @PathVariable("payment_id") UUID paymentId,
            @RequestBody String cancelReason) {

        // TODO: 결제 취소 서비스 로직 호출
        // PaymentResponseDto response = paymentService.cancelPayment(paymentId, cancelReason);
        return CommonApiResponse.success("결제가 취소되었습니다.", null);
    }




}
