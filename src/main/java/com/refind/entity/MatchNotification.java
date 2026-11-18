package com.refind.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "match_notifications", uniqueConstraints = @UniqueConstraint(name = "uq_match_notifications_pair", columnNames = {"lost_report_id", "found_report_id"}))
public class MatchNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lost_report_id", nullable = false)
    private ItemReport lostReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "found_report_id", nullable = false)
    private ItemReport foundReport;

    @Column(name = "match_score", nullable = false)
    private int matchScore;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    protected MatchNotification() { }

    public MatchNotification(ItemReport lostReport, ItemReport foundReport, int matchScore) {
        this.lostReport = lostReport;
        this.foundReport = foundReport;
        this.matchScore = matchScore;
    }

    @PrePersist
    void onCreate() { sentAt = LocalDateTime.now(); }
}
