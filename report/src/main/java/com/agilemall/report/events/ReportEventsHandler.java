package com.agilemall.report.events;

import com.agilemall.common.events.delete.DeletedReportEvent;
import com.agilemall.common.events.delete.FailedDeleteReportEvent;
import com.agilemall.report.entity.Report;
import com.agilemall.report.repository.ReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class ReportEventsHandler {

    private transient  final EventGateway eventGateway;
    private final ReportRepository reportRepository;
    @Autowired
    public ReportEventsHandler(EventGateway eventGateway, ReportRepository reportRepository) {
        this.eventGateway = eventGateway;
        this.reportRepository = reportRepository;
    }

    @EventHandler
    private void on(DeletedReportEvent event) {
        log.info("[@EventHandler] Handle <DeletedReportEvent> for Order Id: {}", event.getOrderId());

        Optional<Report> optReport = reportRepository.findById(event.getReportId());
        if(optReport.isEmpty()) {
            log.info("Can't find Report info for Order Id: {}", event.getOrderId());
            eventGateway.publish(new FailedDeleteReportEvent(event.getReportId(), event.getOrderId()));
            return;
        }

        try {
            reportRepository.delete(optReport.get());
        } catch(Exception e) {
            log.error(e.getMessage());
            eventGateway.publish(new FailedDeleteReportEvent(event.getReportId(), event.getOrderId()));
        }
    }
}
