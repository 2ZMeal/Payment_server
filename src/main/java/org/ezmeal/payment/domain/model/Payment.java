package org.ezmeal.payment.domain.model;

import com.ezmeal.common.entity.BaseEntity; // 공통 모듈 참조
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.ezmeal.payment.domain.model.enums.PaymentMethod;
import org.ezmeal.payment.domain.model.enums.PaymentStatus;
import org.ezmeal.payment.domain.model.enums.PgProvider;

@Entity
@Table(name = "p_payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class Payment extends BaseEntity {



    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Column(name = "payment_key") // DB 컬럼명 지정
    private String paymentKey;

    @Column(name = "pg_transaction_id")
    private String pgTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "pg_provider", nullable = false)
    private PgProvider pgProvider;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Builder
    public Payment(UUID orderId, UUID userId, PaymentStatus status, Integer price,
                   Integer totalPrice, PgProvider pgProvider, PaymentMethod paymentMethod) {

        // 여기에 검증 로직 추가!
        validatePgAndMethod(pgProvider, paymentMethod);

        this.orderId = orderId;
        this.userId = userId;
        this.status = status;
        this.price = price;
        this.totalPrice = totalPrice;
        this.pgProvider = pgProvider;
        this.paymentMethod = paymentMethod;
    }



    /**
     * PG사와 결제 수단 조합 검증
     */
    private void validatePgAndMethod(PgProvider provider, PaymentMethod method) {
        // "너의 주인(validProvider)이 내가 입력받은 provider가 맞니?" 라고 물어보면 끝!
        if (method.getValidProvider() != provider) {
            throw new IllegalArgumentException(
                    String.format("%s 대행사에서 %s 수단을 사용할 수 없습니다.", provider, method)
            );
        }
    }

    // 결제 완료 비즈니스 로직
    public void complete(String pgTransactionId) {
        this.status = PaymentStatus.COMPLETED;
        this.pgTransactionId = pgTransactionId;
        this.paidAt = LocalDateTime.now();
    }

    // 결제 실패/취소 로직
    public void cancel() {
        this.status = PaymentStatus.CANCELLED;
    }


    // 업데이트 상태 로직
    public void updateStatus(PaymentStatus status, String paymentKey) {
        this.status = status;
        if (paymentKey != null) {
            // 별도의 paymentKey 필드를 만들지 않고 기존 pgTransactionId 필드에 저장
            this.pgTransactionId = paymentKey;
        }

        // 만약 상태가 완료라면 시간도 같이 업데이트해주면 좋습니다.
        if (status == PaymentStatus.COMPLETED) {
            this.paidAt = LocalDateTime.now();
        }
    }

}
