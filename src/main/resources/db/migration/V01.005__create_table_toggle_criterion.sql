-- Matching criteria per rule
CREATE TABLE T_toggle_criterion (
    id VARCHAR(36) PRIMARY KEY,
    toggle_rule_id VARCHAR(36) NOT NULL,
    criterion_key VARCHAR(100) NOT NULL,
    criterion_value VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_toggle_criterion_rule_id FOREIGN KEY (toggle_rule_id) REFERENCES T_toggle_rule(id) ON DELETE CASCADE
);

CREATE INDEX I_toggle_criterion_key ON T_toggle_criterion(criterion_key);
