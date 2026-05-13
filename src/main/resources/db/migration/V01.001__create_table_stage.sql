-- Configurable stages with optional inheritance
CREATE TABLE T_stage (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    display_order INT DEFAULT 0,
    parent_stage_id VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT UQ_stage_name UNIQUE (name),
    CONSTRAINT FK_stage_parent_stage_id FOREIGN KEY (parent_stage_id) REFERENCES T_stage(id)
);

CREATE INDEX I_stage_display_order ON T_stage(display_order);
CREATE INDEX I_stage_parent_stage ON T_stage(parent_stage_id);
