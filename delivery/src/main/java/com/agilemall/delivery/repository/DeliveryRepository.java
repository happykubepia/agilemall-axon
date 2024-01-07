package com.agilemall.delivery.repository;

import com.agilemall.delivery.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<Delivery, String> {
    Delivery findByOrderId(String orderId);
}
