package com.refind.repository;

import com.refind.entity.ItemReport;
import com.refind.entity.ReportStatus;
import com.refind.entity.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemReportRepository extends JpaRepository<ItemReport, Long>, JpaSpecificationExecutor<ItemReport> {

    List<ItemReport> findByReportTypeAndStatus(ReportType reportType, ReportStatus status);

    Page<ItemReport> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByReportTypeAndStatus(ReportType reportType, ReportStatus status);

    long countByStatus(ReportStatus status);

    long countByReportType(ReportType reportType);
}
