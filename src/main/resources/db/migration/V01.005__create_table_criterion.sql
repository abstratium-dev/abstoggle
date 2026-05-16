-- Matching criteria per rule
CREATE TABLE T_criterion (
    id VARCHAR(36) PRIMARY KEY,
    rule_id VARCHAR(36) NOT NULL,
    criterion_key VARCHAR(100) NOT NULL,
    criterion_value VARCHAR(500) NOT NULL,
    CONSTRAINT FK_criterion_rule_id FOREIGN KEY (rule_id) REFERENCES T_rule(id) ON DELETE CASCADE
);

CREATE INDEX I_criterion_key ON T_criterion(criterion_key);
