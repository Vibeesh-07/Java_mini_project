package com.refind.repository;

import com.refind.entity.MatchNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchNotificationRepository extends JpaRepository<MatchNotification, Long> {
    boolean existsByLostReportIdAndFoundReportId(Long lostReportId, Long foundReportId);
}
