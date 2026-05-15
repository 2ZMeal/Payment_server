package org.ezmeal.payment.application.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class NicePayApprovalResponse {

    // === 공통 응답 필드 ===
    @JsonProperty("resultCode")
    private String resultCode;  // 결제결과코드 (0000: 성공)

    @JsonProperty("resultMsg")
    private String resultMsg;   // 결제결과메시지

    // === 거래 정보 ===
    @JsonProperty("tid")
    private String tid;         // 결제 승인 키

    @JsonProperty("cancelledTid")
    private String cancelledTid;  // 취소 거래 키

    @JsonProperty("orderId")
    private String orderId;     // 상점 거래 고유번호

    @JsonProperty("ediDate")
    private String ediDate;     // 응답전문생성일시 (ISO 8601)

    @JsonProperty("signature")
    private String signature;   // 위변조 검증 데이터

    @JsonProperty("status")
    private String status;      // paid, ready, failed, cancelled, partialCancelled, expired

    @JsonProperty("paidAt")
    private String paidAt;      // 결제완료시점 (ISO 8601)

    @JsonProperty("failedAt")
    private String failedAt;    // 결제실패시점 (ISO 8601)

    @JsonProperty("cancelledAt")
    private String cancelledAt; // 결제취소시점 (ISO 8601)

    // === 결제 수단 및 금액 정보 ===
    @JsonProperty("payMethod")
    private String payMethod;   // card, vbank, naverpay, kakaopay, payco, ssgpay, samsungpay

    @JsonProperty("amount")
    private Integer amount;     // 결제 금액

    @JsonProperty("balanceAmt")
    private Integer balanceAmt; // 취소 가능 잔액

    @JsonProperty("goodsName")
    private String goodsName;   // 상품명

    @JsonProperty("mallReserved")
    private String mallReserved;  // 상점 정보 전달용 예비필드

    @JsonProperty("useEscrow")
    private Boolean useEscrow;  // 에스크로 거래 여부

    @JsonProperty("currency")
    private String currency;    // KRW, USD, CNY

    @JsonProperty("channel")
    private String channel;     // pc, mobile

    // === 승인 정보 ===
    @JsonProperty("approveNo")
    private String approveNo;   // 제휴사 승인 번호

    // === 구매자 정보 ===
    @JsonProperty("buyerName")
    private String buyerName;   // 구매자 명

    @JsonProperty("buyerTel")
    private String buyerTel;    // 구매자 전화번호

    @JsonProperty("buyerEmail")
    private String buyerEmail;  // 구매자 이메일

    // === 현금영수증 정보 ===
    @JsonProperty("issuedCashReceipt")
    private Boolean issuedCashReceipt;  // 현금영수증 발급여부

    @JsonProperty("receiptUrl")
    private String receiptUrl;  // 매출전표 확인 URL

    // === 상점 정보 ===
    @JsonProperty("mallUserId")
    private String mallUserId;  // 상점에서 관리하는 사용자 아이디

    // === 카드 정보 (신용카드 결제시) ===
    @JsonProperty("card")
    private CardInfo card;      // 신용카드 정보 객체

    // === 가상계좌 정보 (가상계좌 결제시) ===
    @JsonProperty("vbank")
    private VbankInfo vbank;    // 가상계좌 정보 객체

    // === 계좌이체 정보 (계좌이체 결제시) ===
    @JsonProperty("bank")
    private BankInfo bank;      // 은행 정보 객체

    // === 할인 정보 ===
    @JsonProperty("coupon")
    private CouponInfo coupon;  // 즉시할인 프로모션 정보 객체

    // === 취소/환불 정보 ===
    @JsonProperty("cancels")
    private Object cancels;     // 취소 정보 배열

    @JsonProperty("cashReceipts")
    private Object cashReceipts;  // 현금영수증 정보 배열

    // ============ 중첩 클래스 ============

    /**
     * 신용카드 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CardInfo {

        @JsonProperty("cardCode")
        private String cardCode;      // 신용카드사별 코드

        @JsonProperty("cardName")
        private String cardName;      // 결제 카드사 이름

        @JsonProperty("cardNum")
        private String cardNum;       // 카드번호 (마스킹)

        @JsonProperty("cardQuota")
        private String cardQuota;     // 할부개월 (0:일시불)

        @JsonProperty("isInterestFree")
        private Boolean isInterestFree;  // 상점분담무이자 여부

        @JsonProperty("cardType")
        private String cardType;      // credit(신용), check(체크)

        @JsonProperty("canPartCancel")
        private Boolean canPartCancel;  // 부분취소 가능 여부

        @JsonProperty("acquCardCode")
        private String acquCardCode;  // 매입카드사코드

        @JsonProperty("acquCardName")
        private String acquCardName;  // 매입카드사명
    }

    /**
     * 가상계좌 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VbankInfo {

        @JsonProperty("vbankCode")
        private String vbankCode;      // 입금받을 가상계좌 은행코드

        @JsonProperty("vbankName")
        private String vbankName;      // 입금받을 가상계좌 은행명

        @JsonProperty("vbankNumber")
        private String vbankNumber;    // 입금받을 가상계좌 번호

        @JsonProperty("vbankExpDate")
        private String vbankExpDate;   // 가상계좌 입금 만료일 (ISO 8601)

        @JsonProperty("vbankHolder")
        private String vbankHolder;    // 입금받을 가상계좌 예금주명
    }

    /**
     * 은행 정보 (계좌이체)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BankInfo {

        @JsonProperty("bankCode")
        private String bankCode;      // 결제은행코드

        @JsonProperty("bankName")
        private String bankName;      // 결제은행명
    }

    /**
     * 할인 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CouponInfo {

        @JsonProperty("couponAmt")
        private Integer couponAmt;    // 즉시할인 적용된 금액
    }
}
