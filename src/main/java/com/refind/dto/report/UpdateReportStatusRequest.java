package com.refind.dto.report;

import com.refind.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateReportStatusRequest {

    @NotNull(message = "Status is required")
    private ReportStatus status;

    @Size(max = 255, message = "Comments must not exceed 255 characters")
    private String comments;

    public UpdateReportStatusRequest() {
    }

    public UpdateReportStatusRequest(ReportStatus status, String comments) {
        this.status = status;
        this.comments = comments;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
