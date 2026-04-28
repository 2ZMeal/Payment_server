package org.ezmeal.payment.presentation.controller;

import com.ezmeal.common.response.CommonApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.ezmeal.payment.application.dto.request.PaymentConfirmRequestDto;
import org.ezmeal.payment.application.dto.request.PaymentRequestDto;
import org.ezmeal.payment.application.dto.response.PaymentResponseDto;
import org.ezmeal.payment.application.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public CommonApiResponse<PaymentResponseDto> createPayment(@RequestBody PaymentRequestDto requestDto) {
        // TODO: 유저 인증 정보에서 userId를 추출하여 DTO에 세팅하는 로직 필요
        PaymentResponseDto response = paymentService.createPayment(requestDto);
        return CommonApiResponse.success("결제 요청이 성공적으로 생성되었습니다.", response);
    }

    /**
     * 2. 결제 승인 처리 (POST /api/v1/payments/confirm)
     * PG사(토스 등) 인증 완료 후 클라이언트가 전달한 정보로 최종 승인을 진행합니다.
     */
    @PostMapping("/confirm")
    public CommonApiResponse<PaymentResponseDto> confirmPayment(@RequestBody PaymentConfirmRequestDto confirmRequestDto) {
        PaymentResponseDto response = paymentService.approvePayment(
                confirmRequestDto.getPaymentKey(),
                confirmRequestDto.getOrderId(),
                confirmRequestDto.getAmount()
        );
        return CommonApiResponse.success("결제가 최종 승인되었습니다.", response);
    }

    /**
     * 3. 결제 단건 조회 (GET /api/v1/payments/{payment_id})
     */
    @GetMapping("/{payment_id}")
    public CommonApiResponse<PaymentResponseDto> getPayment(@PathVariable("payment_id") UUID paymentId) {
        PaymentResponseDto response = paymentService.getPaymentDetail(paymentId);

        /* [주석 처리: 권한 세분화]
           - 현재 로그인한 유저가 이 결제의 주인인지 확인하는 로직
           - 또는 관리자/업체 권한인지 확인하는 로직
           if (!response.getUserId().equals(currentUserId)) {
               throw new CustomException(PaymentErrorCode.ACCESS_DENIED);
           }
        */

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
