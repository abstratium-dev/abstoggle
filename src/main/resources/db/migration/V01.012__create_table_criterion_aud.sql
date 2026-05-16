-- Envers audit table for T_criterion
CREATE TABLE T_criterion_AUD (
    id VARCHAR(36),
    rule_id VARCHAR(36),
    criterion_key VARCHAR(100),
    criterion_value VARCHAR(500),
    REV BIGINT NOT NULL,
    REVTYPE TINYINT,
    PRIMARY KEY (id, REV),
    CONSTRAINT FK_criterion_aud_rev FOREIGN KEY (REV) REFERENCES REVINFO(REV)
);
