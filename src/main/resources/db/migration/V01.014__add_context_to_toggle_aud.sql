-- Add context column to T_toggle_AUD (Envers audit table)
ALTER TABLE T_toggle_AUD ADD COLUMN context VARCHAR(255);
