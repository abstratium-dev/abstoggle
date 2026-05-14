-- Reusable rule definitions (independent of toggle or stage)
CREATE TABLE T_toggle_rule (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    rule_value VARCHAR(255) DEFAULT 'off',
    description VARCHAR(500),
    CONSTRAINT UQ_toggle_rule_name UNIQUE (name)
);

CREATE INDEX I_toggle_rule_name ON T_toggle_rule(name);
