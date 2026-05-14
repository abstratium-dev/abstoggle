-- Toggle master table
CREATE TABLE T_toggle (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    CONSTRAINT UQ_toggle_name UNIQUE (name)
);

CREATE INDEX I_toggle_name ON T_toggle(name);
CREATE INDEX I_toggle_enabled ON T_toggle(enabled);
