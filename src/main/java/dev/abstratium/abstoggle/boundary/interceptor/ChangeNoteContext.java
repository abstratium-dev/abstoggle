package dev.abstratium.abstoggle.boundary.interceptor;

import jakarta.enterprise.context.RequestScoped;

/**
 * Request-scoped bean to hold the change note for the current request.
 * This allows the RevisionInfoListener to access the change note when creating revisions.
 */
@RequestScoped
public class ChangeNoteContext {
    private String changeNote;

    public String getChangeNote() {
        return changeNote;
    }

    public void setChangeNote(String changeNote) {
        this.changeNote = changeNote;
    }

    public boolean hasChangeNote() {
        return changeNote != null && !changeNote.isBlank();
    }
}
