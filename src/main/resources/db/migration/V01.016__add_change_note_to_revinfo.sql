-- Add change_note to REVINFO for capturing the reason for changes
ALTER TABLE REVINFO ADD COLUMN change_note VARCHAR(255);

CREATE INDEX I_revinfo_change_note ON REVINFO(change_note);
