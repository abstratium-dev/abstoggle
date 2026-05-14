-- Envers audit table for T_toggle_rule
CREATE TABLE T_toggle_rule_AUD (
    id VARCHAR(36),
    name VARCHAR(255),
    rule_value VARCHAR(255),
    description VARCHAR(500),
    REV BIGINT NOT NULL,
    REVTYPE TINYINT,
    PRIMARY KEY (id, REV),
    CONSTRAINT FK_toggle_rule_aud_rev FOREIGN KEY (REV) REFERENCES REVINFO(REV)
);
