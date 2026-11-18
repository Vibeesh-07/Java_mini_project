package com.refind.dto.report;

import jakarta.validation.constraints.Size;

/** Records that a finder has transferred a found item to a campus office. */
public class UpdateCustodyRequest {

    private boolean handedToOffice;

    @Size(max = 120, message = "Office name must not exceed 120 characters")
    private String officeName;

    @Size(max = 160, message = "Office contact must not exceed 160 characters")
    private String officeContact;

    @Size(max = 500, message = "Office notes must not exceed 500 characters")
    private String officeNotes;

    public boolean isHandedToOffice() { return handedToOffice; }
    public void setHandedToOffice(boolean handedToOffice) { this.handedToOffice = handedToOffice; }
    public String getOfficeName() { return officeName; }
    public void setOfficeName(String officeName) { this.officeName = officeName; }
    public String getOfficeContact() { return officeContact; }
    public void setOfficeContact(String officeContact) { this.officeContact = officeContact; }
    public String getOfficeNotes() { return officeNotes; }
    public void setOfficeNotes(String officeNotes) { this.officeNotes = officeNotes; }
}
