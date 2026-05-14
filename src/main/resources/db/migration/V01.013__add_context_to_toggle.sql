-- Add context column to T_toggle for grouping toggles and controlling public API exposure
ALTER TABLE T_toggle ADD COLUMN context VARCHAR(255) NOT NULL DEFAULT '';

CREATE INDEX I_toggle_context ON T_toggle(context);
