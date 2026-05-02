package org.ezmeal.payment.presentation.controller;

import com.ezmeal.common.exception.CustomException;
import com.ezmeal.common.response.CommonApiResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.ezmeal.payment.application.dto.request.PaymentCancelRequest;
import org.ezmeal.payment.application.dto.request.PaymentRequestDto;
import org.ezmeal.payment.application.dto.request.TossConfirmRequest;
import org.ezmeal.payment.application.dto.response.PaymentResponseDto;
import org.ezmeal.payment.application.service.PaymentService;
import org.ezmeal.payment.domain.exception.PaymentErrorCode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
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
// CORS 임시 해제 (프론트와 소통을 위해)
@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*")
public class PaymentController {

    private final PaymentService paymentService;

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

    @GetMapping
    public CommonApiResponse<List<PaymentResponseDto>> getPayments(
            @RequestHeader(value = "X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader
    ) {
        UUID currentUserId = parseUserId(userIdHeader);
        List<PaymentResponseDto> responses = paymentService.getPaymentList(currentUserId, rolesHeader);

        return CommonApiResponse.success(responses);
    }

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
