package com.refind.dto.admin;

import com.refind.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ModerateReportRequest {

    @NotNull(message = "New report status is required")
    private ReportStatus status;

    @Size(max = 255, message = "Moderation note must not exceed 255 characters")
    private String moderationNote;

    public ModerateReportRequest() {
    }

    public ModerateReportRequest(ReportStatus status, String moderationNote) {
        this.status = status;
        this.moderationNote = moderationNote;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public String getModerationNote() {
        return moderationNote;
    }

    public void setModerationNote(String moderationNote) {
        this.moderationNote = moderationNote;
    }
}
