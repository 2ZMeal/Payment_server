package org.ezmeal.payment.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ezmeal.payment.domain.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // 1. 가장 최근에 생성된 결제 데이터 1건만 조회 (상태와 상관없이 최신본)
    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(UUID orderId);

    // 2. 또는 특정 주문의 모든 결제 시도 내역 조회
    List<Payment> findAllByOrderId(UUID orderId);
}
