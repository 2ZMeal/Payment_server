package org.ezmeal.payment.domain.repository;

import org.ezmeal.payment.domain.model.PaymentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface PaymentLogRepository extends JpaRepository<PaymentLog, UUID> {
    // 특정 결제 건에 대한 모든 로그를 시간순으로 조회
    List<PaymentLog> findAllByPaymentOrderByCreatedAtDesc(Object payment);
}
