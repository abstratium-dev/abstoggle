package dev.abstratium.abstoggle.dto;

/**
 * Summary of a single revision from REVINFO, for the history list view.
 */
public class HistoryEntryDto {

    private Long rev;
    private Long timestamp;
    private String username;
    private String changeNote;
    private String correlationId;

    public HistoryEntryDto() {
    }

    public HistoryEntryDto(Long rev, Long timestamp, String username, String changeNote, String correlationId) {
        this.rev = rev;
        this.timestamp = timestamp;
        this.username = username;
        this.changeNote = changeNote;
        this.correlationId = correlationId;
    }

    public Long getRev() {
        return rev;
    }

    public void setRev(Long rev) {
        this.rev = rev;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getChangeNote() {
        return changeNote;
    }

    public void setChangeNote(String changeNote) {
        this.changeNote = changeNote;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
