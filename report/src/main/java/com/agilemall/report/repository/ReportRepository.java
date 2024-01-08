package com.agilemall.report.repository;

import com.agilemall.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, String> {
    public Report findByOrderId(String orderId);
}
