-- Envers audit table for T_rule
CREATE TABLE T_rule_AUD (
    id VARCHAR(36),
    name VARCHAR(255),
    description VARCHAR(500),
    REV BIGINT NOT NULL,
    REVTYPE TINYINT,
    PRIMARY KEY (id, REV),
    CONSTRAINT FK_rule_aud_rev FOREIGN KEY (REV) REFERENCES REVINFO(REV)
);
