-- Envers audit table for T_toggle_rule
CREATE TABLE T_toggle_rule_AUD (
    id VARCHAR(36),
    toggle_stage_id VARCHAR(36),
    rule_value VARCHAR(255),
    description VARCHAR(500),
    priority INT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    REV BIGINT NOT NULL,
    REVTYPE TINYINT,
    PRIMARY KEY (id, REV),
    CONSTRAINT FK_toggle_rule_aud_rev FOREIGN KEY (REV) REFERENCES REVINFO(REV)
);
