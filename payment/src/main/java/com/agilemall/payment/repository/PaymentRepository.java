package com.agilemall.payment.repository;

import com.agilemall.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Payment findByOrderId(String orderId);
}
