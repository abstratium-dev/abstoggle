-- Envers audit table for T_toggle_stage
CREATE TABLE T_toggle_stage_AUD (
    id VARCHAR(36),
    toggle_id VARCHAR(36),
    stage_id VARCHAR(36),
    REV BIGINT NOT NULL,
    REVTYPE TINYINT,
    PRIMARY KEY (id, REV),
    CONSTRAINT FK_toggle_stage_aud_rev FOREIGN KEY (REV) REFERENCES REVINFO(REV)
);
