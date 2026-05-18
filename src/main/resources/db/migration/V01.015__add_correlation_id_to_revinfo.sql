-- Add correlation_id to REVINFO for tracking changes across a single REST call
ALTER TABLE REVINFO ADD COLUMN correlation_id VARCHAR(255);

CREATE INDEX I_revinfo_correlation_id ON REVINFO(correlation_id);
