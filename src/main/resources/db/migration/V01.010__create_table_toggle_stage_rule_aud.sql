-- Envers audit table for T_toggle_stage_rule
CREATE TABLE T_toggle_stage_rule_AUD (
    id VARCHAR(36),
    toggle_id VARCHAR(36),
    stage_id VARCHAR(36),
    rule_id VARCHAR(36),
    rule_value VARCHAR(255),
    priority INT,
    REV BIGINT NOT NULL,
    REVTYPE TINYINT,
    PRIMARY KEY (id, REV),
    CONSTRAINT FK_toggle_stage_rule_aud_rev FOREIGN KEY (REV) REFERENCES REVINFO(REV)
);
