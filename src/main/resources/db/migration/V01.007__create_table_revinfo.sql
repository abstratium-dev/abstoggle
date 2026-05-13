-- Envers revision info table
CREATE TABLE REVINFO (
    REV BIGINT PRIMARY KEY,
    REVTSTMP BIGINT,
    username VARCHAR(255)
);

CREATE INDEX I_revinfo_timestamp ON REVINFO(REVTSTMP);
