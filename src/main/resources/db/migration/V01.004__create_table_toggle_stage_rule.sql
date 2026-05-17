-- Links toggles to stages via rules; a toggle only exists on a stage when at least one rule is assigned
CREATE TABLE T_toggle_stage_rule (
    id VARCHAR(36) PRIMARY KEY,
    toggle_id VARCHAR(36) NOT NULL,
    stage_id VARCHAR(36) NOT NULL,
    rule_id VARCHAR(36) NOT NULL,
    toggle_value VARCHAR(255) DEFAULT 'off',
    priority INT DEFAULT 100,
    CONSTRAINT FK_toggle_stage_rule_toggle_id FOREIGN KEY (toggle_id) REFERENCES T_toggle(id) ON DELETE CASCADE,
    CONSTRAINT FK_toggle_stage_rule_stage_id FOREIGN KEY (stage_id) REFERENCES T_stage(id),
    CONSTRAINT FK_toggle_stage_rule_rule_id FOREIGN KEY (rule_id) REFERENCES T_rule(id),
    CONSTRAINT UQ_toggle_stage_rule_toggle_stage_rule UNIQUE (toggle_id, stage_id, rule_id)
);

CREATE INDEX I_toggle_stage_rule_toggle_stage_priority ON T_toggle_stage_rule(toggle_id, stage_id, priority);
CREATE INDEX I_toggle_stage_rule_stage ON T_toggle_stage_rule(stage_id);
