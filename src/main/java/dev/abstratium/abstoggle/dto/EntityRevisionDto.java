package dev.abstratium.abstoggle.dto;

/**
 * Represents a single revision of a specific entity in the audit history.
 */
public class EntityRevisionDto {
    private long rev;
    private long timestamp;
    private String username;
    private String changeNote;
    private int revtype;
    private String data;

    public EntityRevisionDto(long rev, long timestamp, String username, String changeNote, int revtype, String data) {
        this.rev = rev;
        this.timestamp = timestamp;
        this.username = username;
        this.changeNote = changeNote;
        this.revtype = revtype;
        this.data = data;
    }

    public long getRev() { return rev; }
    public long getTimestamp() { return timestamp; }
    public String getUsername() { return username; }
    public String getChangeNote() { return changeNote; }
    public int getRevtype() { return revtype; }
    public String getData() { return data; }
}
