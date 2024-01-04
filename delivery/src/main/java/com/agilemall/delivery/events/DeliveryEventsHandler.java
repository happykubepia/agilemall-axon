package com.agilemall.delivery.events;

import com.agilemall.common.config.Constants;
import com.agilemall.common.dto.DeliveryStatus;
import com.agilemall.common.events.DeliveryCancelledEvent;
import com.agilemall.common.events.OrderDeliveriedEvent;
import com.agilemall.common.events.ReportUpdateEvent;
import com.agilemall.delivery.entity.Delivery;
import com.agilemall.delivery.repository.DeliveryRepository;
import com.lmax.disruptor.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@EnableRetry
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

    @EventHandler
    @Retryable(
            maxAttempts = Constants.RETRYABLE_MAXATTEMPTS,
            retryFor = { IOException.class, TimeoutException.class, RuntimeException.class},
            backoff = @Backoff(delay = Constants.RETRYABLE_DELAY)
    )
    public void on(ReportUpdateEvent event) {
        log.info("[@EventHandler] Handle ReportUpdateEvent");

    }
}
