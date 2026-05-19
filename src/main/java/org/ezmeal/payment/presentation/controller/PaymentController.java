package org.ezmeal.payment.presentation.controller;

import com.ezmeal.common.exception.CustomException;
import com.ezmeal.common.response.CommonApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ezmeal.payment.application.dto.request.NicePayConfirmRequest;
import org.ezmeal.payment.application.dto.request.PaymentCancelRequest;
import org.ezmeal.payment.application.dto.request.PaymentRequestDto;
import org.ezmeal.payment.application.dto.request.TossConfirmRequest;
import org.ezmeal.payment.application.dto.response.PaymentResponseDto;
import org.ezmeal.payment.application.service.PaymentNiceService;
import org.ezmeal.payment.application.service.PaymentService;
import org.ezmeal.payment.domain.exception.PaymentErrorCode;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
// CORS 임시 해제 (프론트와 소통을 위해)
@CrossOrigin(origins = {"http://localhost:4000", "http://localhost:5173"}, allowedHeaders = "*")

public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentNiceService paymentNiceService;

    @PostMapping
    public CommonApiResponse<PaymentResponseDto> createPayment(
            @RequestBody PaymentRequestDto requestDto,
            @RequestHeader(value = "X-User-Id") String userIdHeader
    ) {
        UUID authenticatedUserId = parseUserId(userIdHeader);
        PaymentResponseDto response = paymentService.createPayment(requestDto, authenticatedUserId);

        return CommonApiResponse.success("결제 요청이 성공적으로 생성되었습니다.", response);
    }



    @PostMapping("/confirm")
    public CommonApiResponse<PaymentResponseDto> confirmPayment(
            @RequestBody TossConfirmRequest requestDto,
            @RequestHeader(value = "X-User-Id") String userIdHeader
    ) {
        UUID currentUserId = parseUserId(userIdHeader);
        PaymentResponseDto response = paymentService.approvePayment(requestDto, currentUserId);

        return CommonApiResponse.success("결제 승인 성공", response);
    }
    /**
     * Toss Pay 결제 승인
     */
    @PostMapping("/confirm/toss")
    public CommonApiResponse<PaymentResponseDto> confirmTossPayment(
            @RequestBody TossConfirmRequest requestDto,
            @RequestHeader(value = "X-User-Id") String userIdHeader
    ) {
        UUID currentUserId = parseUserId(userIdHeader);
        PaymentResponseDto response = paymentService.approvePayment(requestDto, currentUserId);

        return CommonApiResponse.success("Toss Pay 결제 승인 성공", response);
    }

    /**
     * Nice Pay 결제 승인
     */
    @PostMapping("/confirm/nicepay")
    public CommonApiResponse<PaymentResponseDto> confirmNicePayment(
            @RequestBody NicePayConfirmRequest requestDto,
            @RequestHeader(value = "X-User-Id") String userIdHeader
    ) {
        UUID currentUserId = parseUserId(userIdHeader);
        PaymentResponseDto response = paymentNiceService.approveNicePayment(requestDto, currentUserId);

        return CommonApiResponse.success("Nice Pay 결제 승인 성공", response);
    }





    // 단건 조회 (paymentId로)
    @GetMapping("/{payment_id}")
    public CommonApiResponse<PaymentResponseDto> getPayment(
            @PathVariable("payment_id") UUID paymentId,
            @RequestHeader(value = "X-User-Id") String userIdHeader
    ) {
        UUID currentUserId = parseUserId(userIdHeader);
        PaymentResponseDto response = paymentService.getPaymentDetail(paymentId);

        if (!response.getUserId().equals(currentUserId)) {
            throw new CustomException(PaymentErrorCode.ACCESS_DENIED);
        }

        return CommonApiResponse.success(response);
    }

    // 단건 조회 (orderId로) ✅ NEW
    @GetMapping("/order/{order_id}")
    public CommonApiResponse<PaymentResponseDto> getPaymentByOrderId(
            @PathVariable("order_id") UUID orderId,
            @RequestHeader(value = "X-User-Id") String userIdHeader
    ) {
        UUID currentUserId = parseUserId(userIdHeader);
        PaymentResponseDto response = paymentService.getPaymentByOrderId(orderId);

        // 권한 확인: 자신의 결제 정보만 조회 가능
        if (!response.getUserId().equals(currentUserId)) {
            throw new CustomException(PaymentErrorCode.ACCESS_DENIED);
        }

        return CommonApiResponse.success(response);
    }

    // 전체 조회
    @GetMapping
    public CommonApiResponse<List<PaymentResponseDto>> getPayments(
            @RequestHeader(value = "X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader
    ) {
        UUID currentUserId = parseUserId(userIdHeader);
        List<PaymentResponseDto> responses = paymentService.getPaymentList(currentUserId, rolesHeader);

        return CommonApiResponse.success(responses);
    }



    //취소
    @PostMapping("/{payment_id}/cancel")
    public CommonApiResponse<PaymentResponseDto> cancelPayment(
            @PathVariable("payment_id") UUID paymentId,
            @RequestBody PaymentCancelRequest request,
            @RequestHeader(value = "X-User-Id") String userIdHeader
    ) {
        UUID currentUserId = parseUserId(userIdHeader);
        PaymentResponseDto response = paymentService.cancelPayment(
                paymentId,
                request.getCancelReason(),
                currentUserId);

        return CommonApiResponse.success("결제가 취소되었습니다.", response);
    }

    private UUID parseUserId(String userIdHeader) {
        try {
            return UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CustomException(PaymentErrorCode.INVALID_USER_ID);
        }
    }

}
