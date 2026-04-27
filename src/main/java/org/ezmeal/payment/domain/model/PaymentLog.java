package org.ezmeal.payment.domain.model;

import com.ezmeal.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;
import org.ezmeal.payment.domain.model.enums.LogType;
import org.ezmeal.payment.domain.model.enums.PaymentMethod;
import org.ezmeal.payment.domain.model.enums.PaymentStatus;

@Entity
@Table(name = "p_payment_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PaymentLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_log_id")
    private UUID paymentLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "log_type", nullable = false)
    private LogType logType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "request_data", columnDefinition = "TEXT")
    private String requestData;

    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;
}

