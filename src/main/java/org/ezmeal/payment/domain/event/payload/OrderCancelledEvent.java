package org.ezmeal.payment.domain.event.payload;

import com.ezmeal.common.message.DomainEvent;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * [역할]
 * Order Service에서 주문 취소 시 발행하는 이벤트
 * - Payment Service가 구독하여 결제 취소 처리
 * - Shipment Service가 구독하여 배달 취소 처리
 *
 * [발행 시점]
 * Order Service: OrderService.cancelOrder()
 *
 * [Kafka Topic]
 * order.cancelled
 *
 * [구독 서비스]
 * Payment Service: OrderEventListener.handleOrderCancelledEvent()
 * Shipment Service: 배달 취소 처리
 * Product Service: 재고 복구
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEvent implements DomainEvent, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 주문 ID
     */
    private UUID orderId;

    /**
     * 사용자 ID
     */
    private String userId;

    /**
     * 누가 취소했는가
     * - "USER" (사용자 요청)
     * - "ADMIN" (관리자)
     * - "SYSTEM" (시스템)
     */
    private String cancelledBy;

    /**
     * 결제 취소 필요 여부
     * - true: payment-service가 결제 취소 처리
     * - false: 결제 전(READY) 취소이므로 결제 취소 불필요
     */
    private boolean requiresPaymentCancellation;

    /**
     * 배달 취소 필요 여부
     * - true: shipment-service가 배달 취소 처리 (CONFIRMED 상태에서 취소)
     * - false: 아직 배달 요청이 안 간 상태 (READY/PENDING)
     */
    private boolean requiresShipmentCancellation;

    /**
     * 재고 복구 필요 여부
     * - true: product-service가 재고 복구 처리
     * - false: 재고 예약 전(READY) 취소이므로 복구 불필요
     */
    private boolean requiresStockRestore;

    /**
     * 복구 대상 상품 목록
     * - product-service가 어떤 상품의 재고를 복구할지 알 수 있도록 전달
     */
    private List<StockRestoreItem> stockRestoreItems;

    /**
     * 이벤트 발생 시간
     */
    private LocalDateTime occurredAt;

    /**
     * 재고 복구 대상 상품 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StockRestoreItem implements Serializable {
        /**
         * 상품 ID
         */
        private UUID productId;

        /**
         * 복구 수량
         */
        private Integer quantity;
    }
}