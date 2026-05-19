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
 * Order Service에서 주문 생성 시 발행하는 이벤트
 * - Payment Service가 구독하여 결제 요청 생성
 *
 * [발행 시점]
 * Order Service: OrderService.createOrder()
 *
 * [Kafka Topic]
 * order.created
 *
 * [구독 서비스]
 * Payment Service: OrderEventListener.handleOrderCreatedEvent()
 */


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCreatedEvent implements DomainEvent, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 주문 ID
     */
    private UUID orderId;

    /**
     * 회사 ID
     * - 어느 매장인지 구분
     */
    private UUID companyId;

    /**
     * 사용자 ID
     */
    private String userId;

    /**
     * 총 주문 금액
     * - 배송료 포함
     */
    private Integer totalPrice;

    /**
     * 배달 주소
     */
    private String deliveryAddress;

    /**
     * 주문 상품 목록
     */
    private List<OrderItemPayload> items;

    /**
     * 이벤트 발생 시간
     */
    private LocalDateTime occurredAt;

    /**
     * 주문 상품 정보
     */


    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemPayload implements Serializable {
        /**
         * 상품명
         */
        private String productName;

        /**
         * 상품 가격
         */
        private Integer productPrice;

        /**
         * 주문 수량
         */
        private Integer quantity;
    }
}