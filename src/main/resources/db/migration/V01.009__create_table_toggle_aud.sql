-- Envers audit table for T_toggle
CREATE TABLE T_toggle_AUD (
    id VARCHAR(36),
    name VARCHAR(255),
    description TEXT,
    enabled BOOLEAN,
    REV BIGINT NOT NULL,
    REVTYPE TINYINT,
    PRIMARY KEY (id, REV),
    CONSTRAINT FK_toggle_aud_rev FOREIGN KEY (REV) REFERENCES REVINFO(REV)
);
