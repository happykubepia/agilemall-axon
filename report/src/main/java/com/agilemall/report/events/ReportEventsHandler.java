package com.agilemall.report.events;

import com.agilemall.common.events.ReportCreatedEvent;
import com.agilemall.common.events.ReportDeliveryStatusUpdatedEvent;
import com.agilemall.report.entity.Report;
import com.agilemall.report.repository.ReportRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReportEventsHandler {
    @Autowired
    ReportRepository reportRepository;

    @EventHandler
    private void on(ReportCreatedEvent event) {
        log.info("[@EventHandler] Handle <ReportCreatedEvent>");
        log.info(event.toString());

        Report report = new Report();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            BeanUtils.copyProperties(event, report);
            report.setOrderDetails(gson.toJson(event.getOrderDetails()));
            report.setPaymentDetails(gson.toJson(event.getPaymentDetails()));

            reportRepository.save(report);
        } catch(Exception e) {
            log.error("Error is occurred during handle <ReportCreatedEvent>: {}", e.getMessage());
        }
    }

    @EventHandler
    private void on(ReportDeliveryStatusUpdatedEvent event) {
        log.info("[@EventHandler] Handle <ReportDeliveryStatusUpdatedEvent> for Order Id: {}", event.getOrderId());

        Report report = reportRepository.findByOrderId(event.getOrderId());
        if(report != null) {
            report.setDeliveryStatus(event.getDeliveryStatus());
            reportRepository.save(report);
        }
    }
}
