-- Envers audit table for T_toggle_criterion
CREATE TABLE T_toggle_criterion_AUD (
    id VARCHAR(36),
    toggle_rule_id VARCHAR(36),
    criterion_key VARCHAR(100),
    criterion_value VARCHAR(500),
    created_at TIMESTAMP,
    REV BIGINT NOT NULL,
    REVTYPE TINYINT,
    PRIMARY KEY (id, REV),
    CONSTRAINT FK_toggle_criterion_aud_rev FOREIGN KEY (REV) REFERENCES REVINFO(REV)
);
