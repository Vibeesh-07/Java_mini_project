ALTER TABLE users ADD COLUMN contact_phone VARCHAR(25) NULL AFTER email;

ALTER TABLE item_reports
    ADD COLUMN handed_to_office BOOLEAN NOT NULL DEFAULT FALSE AFTER status,
    ADD COLUMN office_name VARCHAR(120) NULL AFTER handed_to_office,
    ADD COLUMN office_contact VARCHAR(160) NULL AFTER office_name,
    ADD COLUMN office_notes VARCHAR(500) NULL AFTER office_contact,
    ADD COLUMN handed_over_at TIMESTAMP NULL AFTER office_notes;

CREATE TABLE match_notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lost_report_id BIGINT NOT NULL,
    found_report_id BIGINT NOT NULL,
    match_score INT NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_match_notifications_lost FOREIGN KEY (lost_report_id) REFERENCES item_reports(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_notifications_found FOREIGN KEY (found_report_id) REFERENCES item_reports(id) ON DELETE CASCADE,
    CONSTRAINT uq_match_notifications_pair UNIQUE (lost_report_id, found_report_id),
    INDEX idx_match_notifications_lost (lost_report_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
