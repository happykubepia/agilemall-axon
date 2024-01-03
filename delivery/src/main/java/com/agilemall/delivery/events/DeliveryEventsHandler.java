package com.agilemall.delivery.events;

import com.agilemall.common.dto.DeliveryStatus;
import com.agilemall.common.events.DeliveryCancelledEvent;
import com.agilemall.common.events.OrderDeliveriedEvent;
import com.agilemall.delivery.entity.Delivery;
import com.agilemall.delivery.repository.DeliveryRepository;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DeliveryEventsHandler {
    @Autowired
    private DeliveryRepository deliveryRepository;

    @EventHandler
    public void on(OrderDeliveriedEvent event) {
        log.info("[@EventHandler] Handle OrderDeliveriedEvent");

        Delivery delivery = new Delivery();
        BeanUtils.copyProperties(event, delivery);
        deliveryRepository.save(delivery);
    }

    @EventHandler
    public void on(DeliveryCancelledEvent event) {
        log.info("[@EventHandler] Handle DeliveryCancelledEvent");

        Delivery delivery = deliveryRepository.findById(event.getDeliveryId()).get();
        delivery.setDeliveryStatus(DeliveryStatus.CANCELED.value());
        deliveryRepository.save(delivery);
    }
}
