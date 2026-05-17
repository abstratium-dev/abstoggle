package dev.abstratium.abstoggle.dto;

/**
 * A single audited row change within a revision, for the history detail view.
 * REVTYPE: 0 = ADD, 1 = MOD, 2 = DEL
 */
public class HistoryChangeDto {

    private String table;
    private String entityId;
    private int revtype;
    private String data;

    public HistoryChangeDto() {
    }

    public HistoryChangeDto(String table, String entityId, int revtype, String data) {
        this.table = table;
        this.entityId = entityId;
        this.revtype = revtype;
        this.data = data;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public int getRevtype() {
        return revtype;
    }

    public void setRevtype(int revtype) {
        this.revtype = revtype;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
