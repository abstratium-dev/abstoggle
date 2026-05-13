-- Rules per stage
CREATE TABLE T_toggle_rule (
    id VARCHAR(36) PRIMARY KEY,
    toggle_stage_id VARCHAR(36) NOT NULL,
    rule_value VARCHAR(255) DEFAULT 'off',
    description VARCHAR(500),
    priority INT DEFAULT 100,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_toggle_rule_stage_id FOREIGN KEY (toggle_stage_id) REFERENCES T_toggle_stage(id) ON DELETE CASCADE
);

CREATE INDEX I_toggle_rule_stage_priority ON T_toggle_rule(toggle_stage_id, priority);
