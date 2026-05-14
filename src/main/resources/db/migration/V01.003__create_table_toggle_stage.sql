-- Stage definitions per toggle
CREATE TABLE T_toggle_stage (
    id VARCHAR(36) PRIMARY KEY,
    toggle_id VARCHAR(36) NOT NULL,
    stage_id VARCHAR(36) NOT NULL,
    CONSTRAINT FK_toggle_stage_toggle_id FOREIGN KEY (toggle_id) REFERENCES T_toggle(id) ON DELETE CASCADE,
    CONSTRAINT FK_toggle_stage_stage_id FOREIGN KEY (stage_id) REFERENCES T_stage(id),
    CONSTRAINT UQ_toggle_stage_toggle_stage UNIQUE (toggle_id, stage_id)
);

CREATE INDEX I_toggle_stage_stage ON T_toggle_stage(stage_id);
