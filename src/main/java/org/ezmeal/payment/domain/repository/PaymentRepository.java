package org.ezmeal.payment.domain.repository;

import org.ezmeal.payment.domain.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // 주문 ID로 결제 내역을 찾아야 할 때가 많으므로 추가
    Optional<Payment> findByOrderId(UUID orderId);
}
