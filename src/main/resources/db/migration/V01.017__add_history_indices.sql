-- Add indices for history/audit table performance

-- Indices on AUD tables for searching by REV (revision number)
CREATE INDEX I_stage_aud_rev ON T_stage_AUD(REV);
CREATE INDEX I_toggle_aud_rev ON T_toggle_AUD(REV);
CREATE INDEX I_toggle_stage_rule_aud_rev ON T_toggle_stage_rule_AUD(REV);
CREATE INDEX I_rule_aud_rev ON T_rule_AUD(REV);
CREATE INDEX I_criterion_aud_rev ON T_criterion_AUD(REV);

-- Indices on AUD tables for searching by id (entity id)
CREATE INDEX I_stage_aud_id ON T_stage_AUD(id);
CREATE INDEX I_toggle_aud_id ON T_toggle_AUD(id);
CREATE INDEX I_toggle_stage_rule_aud_id ON T_toggle_stage_rule_AUD(id);
CREATE INDEX I_rule_aud_id ON T_rule_AUD(id);
CREATE INDEX I_criterion_aud_id ON T_criterion_AUD(id);
