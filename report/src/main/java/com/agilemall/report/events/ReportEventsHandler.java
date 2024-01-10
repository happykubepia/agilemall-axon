package com.agilemall.report.events;

import com.agilemall.common.events.create.CreatedReportEvent;
import com.agilemall.common.events.update.UpdatedReportDeliveryStatusEvent;
import com.agilemall.common.events.update.UpdatedReportEvent;
import com.agilemall.report.entity.Report;
import com.agilemall.report.repository.ReportRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class ReportEventsHandler {
    @Autowired
    ReportRepository reportRepository;

    @EventHandler
    private void on(CreatedReportEvent event) {
        log.info("[@EventHandler] Handle <CreatedReportEvent>");
        log.info(event.toString());

        Report report = new Report();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            BeanUtils.copyProperties(event, report);
            report.setOrderDetails(gson.toJson(event.getOrderDetails()));
            report.setPaymentDetails(gson.toJson(event.getPaymentDetails()));

            reportRepository.save(report);
        } catch(Exception e) {
            log.error("Error is occurred during handle <CreatedReportEvent>: {}", e.getMessage());
        }
    }
    @EventHandler
    private void on(UpdatedReportEvent event) {
        log.info("[@EventHandler] Handle <UpdatedReportEvent>");
        log.info(event.toString());

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Optional<Report> optReport = reportRepository.findById(event.getReportId());
        if(optReport.isEmpty()) {
            log.info("Can't find Report info for Report Id:{}", event.getReportId());
            return;
        }
        Report report = optReport.get();
        try {
            report.setOrderDatetime(event.getOrderDatetime());
            report.setTotalOrderAmt(event.getTotalOrderAmt());
            report.setOrderStatus(event.getOrderStatus());
            report.setOrderDetails(gson.toJson(event.getOrderDetails()));
            report.setTotalPaymentAmt(event.getTotalPaymentAmt());
            report.setPaymentStatus(event.getPaymentStatus());
            report.setPaymentDetails(gson.toJson(event.getPaymentDetails()));

            reportRepository.save(report);
        } catch(Exception e) {
            log.error("Error is occurred during handle <UpdatedReportEvent>: {}", e.getMessage());
        }
    }

    @EventHandler
    private void on(UpdatedReportDeliveryStatusEvent event) {
        log.info("[@EventHandler] Handle <UpdatedReportDeliveryStatusEvent> for Order Id: {}", event.getOrderId());

        Optional<Report> optReport = reportRepository.findById(event.getReportId());
        if(optReport.isPresent()) {
            Report report = optReport.get();
            report.setDeliveryStatus(event.getDeliveryStatus());
            reportRepository.save(report);
        }
    }
}
