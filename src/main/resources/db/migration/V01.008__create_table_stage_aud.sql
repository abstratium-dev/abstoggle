-- Envers audit table for T_stage
CREATE TABLE T_stage_AUD (
    id VARCHAR(36),
    name VARCHAR(100),
    description VARCHAR(500),
    display_order INT,
    parent_stage_id VARCHAR(36),
    created_at TIMESTAMP,
    REV BIGINT NOT NULL,
    REVTYPE TINYINT,
    PRIMARY KEY (id, REV),
    CONSTRAINT FK_stage_aud_rev FOREIGN KEY (REV) REFERENCES REVINFO(REV)
);
