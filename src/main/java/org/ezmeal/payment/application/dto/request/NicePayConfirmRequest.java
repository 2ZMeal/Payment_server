package org.ezmeal.payment.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Nice Pay 결제 승인 요청 DTO
 *
 * 프론트엔드에서 Nice Pay 결제창 인증 후,
 * returnUrl로 POST된 데이터를 받아서 처리하는 DTO
 *
 * 흐름:
 * 1. 프론트: AUTHNICE.requestPay() 호출
 * 2. 사용자 인증 완료
 * 3. returnUrl로 POST 요청 (이 데이터 포함)
 * 4. 백엔드: 이 DTO로 검증 및 서명 검증
 * 5. Nice Pay API 호출로 최종 승인
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NicePayConfirmRequest {

    /**
     * 인증 결과 코드
     * - 0000: 인증 성공 (이 경우만 승인 API 호출)
     * - 그 외: 인증 실패
     */
    @NotBlank(message = "authResultCode는 필수입니다.")
    private String authResultCode;

    /**
     * 인증 결과 메시지
     * 예: "인증 성공", "사용자 취소" 등
     */
    private String authResultMsg;

    /**
     * Nice Pay 거래 ID
     * - 결제 승인 API 호출시 URL에 사용: /v1/payments/{tid}
     * - 필수 필드
     */
    @NotBlank(message = "tid는 필수입니다.")
    private String tid;

    /**
     * 클라이언트 ID
     * - 가맹점 식별코드
     * - 검증용으로 사용
     */
    private String clientId;

    /**
     * 주문 ID
     * - 프론트에서 요청시 전달했던 orderId
     * - 검증: DB의 주문정보와 일치 확인
     */
    @NotBlank(message = "orderId는 필수입니다.")
    private String orderId;

    /**
     * 결제 금액
     * - 프론트에서 요청시 전달했던 amount
     * - 검증: 금액 위변조 확인 (서명 검증 시 사용)
     */
    @NotNull(message = "amount는 필수입니다.")
    @Positive(message = "amount는 양수여야 합니다.")
    private Integer amount;

    /**
     * 상점 예약필드
     * - 프론트에서 요청시 전달한 mallReserved
     * - 선택사항 (null 가능)
     */
    private String mallReserved;

    /**
     * 인증 TOKEN
     * - 위변조 검증에 사용
     * - 서명 생성: hex(sha256(authToken + clientId + amount + secretKey))
     */
    @NotBlank(message = "authToken은 필수입니다.")
    private String authToken;

    /**
     * 위변조 검증 데이터 (서명)
     * - 생성규칙: hex(sha256(authToken + clientId + amount + secretKey))
     * - 백엔드에서 같은 규칙으로 계산하여 비교
     * - 일치하지 않으면 승인 불가
     */
    @NotBlank(message = "signature는 필수입니다.")
    private String signature;

    /**
     * 검증 메서드
     * - authResultCode가 0000인지 확인
     * @return true: 인증 성공, false: 인증 실패
     */
    public boolean isAuthSuccess() {
        return "0000".equals(this.authResultCode);
    }

    /**
     * toString 오버라이드
     * - 보안: authToken, signature는 마스킹
     */
    @Override
    public String toString() {
        return "NicePayConfirmRequest{" +
                "authResultCode='" + authResultCode + '\'' +
                ", authResultMsg='" + authResultMsg + '\'' +
                ", tid='" + tid + '\'' +
                ", clientId='" + clientId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", amount=" + amount +
                ", mallReserved='" + mallReserved + '\'' +
                ", authToken='" + (authToken != null ? "***" : null) + '\'' +
                ", signature='" + (signature != null ? "***" : null) + '\'' +
                '}';
    }
}
