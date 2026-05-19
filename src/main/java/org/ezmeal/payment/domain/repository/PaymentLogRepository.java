package org.ezmeal.payment.domain.repository;

import java.util.List;
import java.util.UUID;
import org.ezmeal.payment.domain.model.Payment;
import org.ezmeal.payment.domain.model.PaymentLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentLogRepository extends JpaRepository<PaymentLog, UUID> {
    // 특정 결제 건에 대한 모든 로그를 시간순으로 조회
    List<PaymentLog> findAllByPaymentOrderByCreatedAtDesc(Payment payment);

}
