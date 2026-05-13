-- Envers audit table for T_toggle
CREATE TABLE T_toggle_AUD (
    id VARCHAR(36),
    name VARCHAR(255),
    description TEXT,
    created_by VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    enabled BOOLEAN,
    REV BIGINT NOT NULL,
    REVTYPE TINYINT,
    PRIMARY KEY (id, REV),
    CONSTRAINT FK_toggle_aud_rev FOREIGN KEY (REV) REFERENCES REVINFO(REV)
);
