-- Reusable rule definitions (independent of toggle or stage)
CREATE TABLE T_rule (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    CONSTRAINT UQ_rule_name UNIQUE (name)
);

CREATE INDEX I_rule_name ON T_rule(name);
