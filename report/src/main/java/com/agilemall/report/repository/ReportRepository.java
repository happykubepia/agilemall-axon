package com.agilemall.report.repository;

import com.agilemall.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, String> {
    Optional<Report> findByOrderId(String orderId);
}
