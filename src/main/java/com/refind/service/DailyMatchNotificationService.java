package com.refind.service;

import com.refind.dto.matching.MatchResultResponse;
import com.refind.entity.ItemReport;
import com.refind.entity.MatchNotification;
import com.refind.entity.ReportStatus;
import com.refind.entity.ReportType;
import com.refind.matching.MatchingService;
import com.refind.repository.ItemReportRepository;
import com.refind.repository.MatchNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Sends one email per newly discovered LOST/FOUND pair; a database constraint prevents repeat mail. */
@Service
@ConditionalOnProperty(prefix = "app.notifications", name = "enabled", havingValue = "true")
public class DailyMatchNotificationService {
    private static final Logger log = LoggerFactory.getLogger(DailyMatchNotificationService.class);
    private final ItemReportRepository reportRepository;
    private final MatchNotificationRepository notificationRepository;
    private final MatchingService matchingService;
    private final JavaMailSender mailSender;
    private final String from;

    public DailyMatchNotificationService(ItemReportRepository reportRepository,
                                         MatchNotificationRepository notificationRepository,
                                         MatchingService matchingService,
                                         JavaMailSender mailSender,
                                         @Value("${app.notifications.from}") String from) {
        this.reportRepository = reportRepository;
        this.notificationRepository = notificationRepository;
        this.matchingService = matchingService;
        this.mailSender = mailSender;
        this.from = from;
    }

    @Scheduled(cron = "${app.notifications.cron}")
    @Transactional
    public void sendDailyMatchAlerts() {
        List<ItemReport> lostReports = new ArrayList<>(reportRepository.findByReportTypeAndStatus(ReportType.LOST, ReportStatus.OPEN));
        lostReports.addAll(reportRepository.findByReportTypeAndStatus(ReportType.LOST, ReportStatus.CLAIM_PENDING));
        for (ItemReport lostReport : lostReports) {
            for (MatchResultResponse match : matchingService.findMatches(lostReport, lostReport.getUser().getId())) {
                ItemReport foundReport = reportRepository.findById(match.getReport().getId()).orElse(null);
                if (foundReport != null) {
                    sendIfNew(lostReport, foundReport, match.getMatchScore());
                }
            }
        }
    }

    private void sendIfNew(ItemReport lostReport, ItemReport foundReport, int score) {
        if (notificationRepository.existsByLostReportIdAndFoundReportId(lostReport.getId(), foundReport.getId())) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(lostReport.getUser().getEmail());
        message.setSubject("PSG iTech Lost & Found: possible match for your item");
        message.setText("Hello " + lostReport.getUser().getName() + ",\n\n"
                + "A found-item report may match your lost item ‘" + lostReport.getTitle() + "’ (match score " + score + "/100).\n"
                + "Found item: " + foundReport.getTitle() + "\n"
                + "View it in the PSG iTech Lost & Found portal and contact the reporter safely.\n\n"
                + "This is an automated PSG iTech lost-and-found notification.");
        try {
            mailSender.send(message);
            notificationRepository.save(new MatchNotification(lostReport, foundReport, score));
            log.info("Sent match alert for lost report {} and found report {}", lostReport.getId(), foundReport.getId());
        } catch (Exception ex) {
            // Do not record a failed delivery; the next daily run can retry it.
            log.warn("Could not send match alert for lost report {}: {}", lostReport.getId(), ex.getMessage());
        }
    }
}
