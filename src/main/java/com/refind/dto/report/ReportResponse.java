package com.refind.dto.report;

import com.refind.entity.ItemReport;
import com.refind.entity.ReportStatus;
import com.refind.entity.ReportType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReportResponse {

    private Long id;
    private ReportType reportType;
    private String title;
    private String description;
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private String color;
    private String brand;
    private String locationName;
    private String locationZone;
    private LocalDate incidentDate;
    private ReportStatus status;
    private Long userId;
    private String userName;
    private String contactEmail;
    private String contactPhone;
    private boolean contactAvailable;
    private boolean handedToOffice;
    private String officeName;
    private String officeContact;
    private String officeNotes;
    private LocalDateTime handedOverAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isOwner;
    private boolean hasImage;
    private String imageUrl;

    public ReportResponse() {
    }

    public static ReportResponse fromEntity(ItemReport report, Long currentUserId) {
        if (report == null) return null;
        ReportResponse dto = new ReportResponse();
        dto.setId(report.getId());
        dto.setReportType(report.getReportType());
        dto.setTitle(report.getTitle());
        dto.setDescription(report.getDescription());

        if (report.getCategory() != null) {
            dto.setCategoryId(report.getCategory().getId());
            dto.setCategoryName(report.getCategory().getName());
            dto.setCategoryIcon(report.getCategory().getIcon());
        }

        dto.setColor(report.getColor());
        dto.setBrand(report.getBrand());
        dto.setLocationName(report.getLocationName());
        dto.setLocationZone(report.getLocationZone());
        dto.setIncidentDate(report.getIncidentDate());
        dto.setStatus(report.getStatus());

        if (report.getUser() != null) {
            dto.setUserId(report.getUser().getId());
            dto.setUserName(report.getUser().getName());
            dto.setOwner(currentUserId != null && currentUserId.equals(report.getUser().getId()));
            // Keep contact details out of public listings to prevent scraping.
            if (currentUserId != null) {
                dto.setContactEmail(report.getUser().getEmail());
                dto.setContactPhone(report.getUser().getContactPhone());
                dto.setContactAvailable(true);
            }
        }

        dto.setHandedToOffice(report.isHandedToOffice());
        dto.setOfficeName(report.getOfficeName());
        dto.setOfficeContact(report.getOfficeContact());
        dto.setOfficeNotes(report.getOfficeNotes());
        dto.setHandedOverAt(report.getHandedOverAt());

        dto.setCreatedAt(report.getCreatedAt());
        dto.setUpdatedAt(report.getUpdatedAt());

        boolean hasImg = report.getImageData() != null && !report.getImageData().isBlank();
        dto.setHasImage(hasImg);
        dto.setImageUrl(hasImg ? "/api/reports/" + report.getId() + "/image" : null);

        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public void setReportType(ReportType reportType) {
        this.reportType = reportType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryIcon() {
        return categoryIcon;
    }

    public void setCategoryIcon(String categoryIcon) {
        this.categoryIcon = categoryIcon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getLocationZone() {
        return locationZone;
    }

    public void setLocationZone(String locationZone) {
        this.locationZone = locationZone;
    }

    public LocalDate getIncidentDate() {
        return incidentDate;
    }

    public void setIncidentDate(LocalDate incidentDate) {
        this.incidentDate = incidentDate;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public boolean isContactAvailable() { return contactAvailable; }
    public void setContactAvailable(boolean contactAvailable) { this.contactAvailable = contactAvailable; }
    public boolean isHandedToOffice() { return handedToOffice; }
    public void setHandedToOffice(boolean handedToOffice) { this.handedToOffice = handedToOffice; }
    public String getOfficeName() { return officeName; }
    public void setOfficeName(String officeName) { this.officeName = officeName; }
    public String getOfficeContact() { return officeContact; }
    public void setOfficeContact(String officeContact) { this.officeContact = officeContact; }
    public String getOfficeNotes() { return officeNotes; }
    public void setOfficeNotes(String officeNotes) { this.officeNotes = officeNotes; }
    public LocalDateTime getHandedOverAt() { return handedOverAt; }
    public void setHandedOverAt(LocalDateTime handedOverAt) { this.handedOverAt = handedOverAt; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isOwner() {
        return isOwner;
    }

    public void setOwner(boolean owner) {
        isOwner = owner;
    }

    public boolean isHasImage() {
        return hasImage;
    }

    public void setHasImage(boolean hasImage) {
        this.hasImage = hasImage;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
