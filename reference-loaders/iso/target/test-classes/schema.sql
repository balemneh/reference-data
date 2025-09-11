-- Create schemas
CREATE SCHEMA IF NOT EXISTS reference_data;
CREATE SCHEMA IF NOT EXISTS staging;

-- Create code_system table
CREATE TABLE IF NOT EXISTS reference_data.code_system (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create countries_v table (versioned/bitemporal)
CREATE TABLE IF NOT EXISTS reference_data.countries_v (
    id UUID PRIMARY KEY,
    code_system_id UUID NOT NULL REFERENCES reference_data.code_system(id),
    country_code VARCHAR(10) NOT NULL,
    country_name VARCHAR(255) NOT NULL,
    iso2_code VARCHAR(2),
    iso3_code VARCHAR(3),
    numeric_code VARCHAR(3),
    is_active BOOLEAN DEFAULT true,
    metadata JSONB,
    version BIGINT DEFAULT 1,
    valid_from DATE NOT NULL,
    valid_to DATE,
    recorded_at TIMESTAMP NOT NULL,
    recorded_by VARCHAR(100),
    change_request_id VARCHAR(50),
    CONSTRAINT uk_country_version UNIQUE (country_code, code_system_id, version)
);

-- Create index for current records
CREATE INDEX idx_country_code_system ON reference_data.countries_v(country_code, code_system_id, valid_from);
CREATE INDEX idx_country_valid_dates ON reference_data.countries_v(valid_from, valid_to);

-- Create view for current countries
CREATE OR REPLACE VIEW reference_data.countries_current AS
SELECT * FROM reference_data.countries_v
WHERE valid_to IS NULL OR valid_to > CURRENT_DATE;

-- Create staging table
CREATE TABLE IF NOT EXISTS staging.iso_countries_staging (
    id BIGSERIAL PRIMARY KEY,
    country_name VARCHAR(255) NOT NULL,
    alpha2_code VARCHAR(2) NOT NULL,
    alpha3_code VARCHAR(3) NOT NULL,
    numeric_code VARCHAR(3),
    official_name VARCHAR(500),
    common_name VARCHAR(255),
    capital VARCHAR(255),
    region VARCHAR(100),
    subregion VARCHAR(100),
    continent VARCHAR(50),
    is_independent BOOLEAN,
    is_un_member BOOLEAN,
    currency_code VARCHAR(3),
    currency_name VARCHAR(100),
    phone_code VARCHAR(20),
    tld VARCHAR(10),
    languages TEXT,
    population BIGINT,
    area_sq_km DOUBLE PRECISION,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    source_file VARCHAR(500),
    source_date VARCHAR(50),
    load_execution_id VARCHAR(36),
    loaded_at TIMESTAMP,
    source_hash VARCHAR(64),
    validation_status VARCHAR(20),
    validation_errors TEXT,
    processing_status VARCHAR(20),
    processing_notes TEXT
);

-- Create index for staging
CREATE INDEX idx_iso_staging_alpha2 ON staging.iso_countries_staging(alpha2_code);
CREATE INDEX idx_iso_staging_alpha3 ON staging.iso_countries_staging(alpha3_code);
CREATE INDEX idx_iso_staging_exec ON staging.iso_countries_staging(load_execution_id);

-- Spring Batch tables (simplified)
CREATE TABLE IF NOT EXISTS BATCH_JOB_INSTANCE (
    JOB_INSTANCE_ID BIGSERIAL PRIMARY KEY,
    VERSION BIGINT,
    JOB_NAME VARCHAR(100) NOT NULL,
    JOB_KEY VARCHAR(32) NOT NULL,
    CONSTRAINT JOB_INST_UN UNIQUE (JOB_NAME, JOB_KEY)
);

CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION (
    JOB_EXECUTION_ID BIGSERIAL PRIMARY KEY,
    VERSION BIGINT,
    JOB_INSTANCE_ID BIGINT NOT NULL,
    CREATE_TIME TIMESTAMP NOT NULL,
    START_TIME TIMESTAMP,
    END_TIME TIMESTAMP,
    STATUS VARCHAR(10),
    EXIT_CODE VARCHAR(2500),
    EXIT_MESSAGE VARCHAR(2500),
    LAST_UPDATED TIMESTAMP,
    CONSTRAINT JOB_INST_EXEC_FK FOREIGN KEY (JOB_INSTANCE_ID)
        REFERENCES BATCH_JOB_INSTANCE(JOB_INSTANCE_ID)
);

CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION_PARAMS (
    JOB_EXECUTION_ID BIGINT NOT NULL,
    PARAMETER_NAME VARCHAR(100) NOT NULL,
    PARAMETER_TYPE VARCHAR(100) NOT NULL,
    PARAMETER_VALUE VARCHAR(2500),
    IDENTIFYING CHAR(1) NOT NULL,
    CONSTRAINT JOB_EXEC_PARAMS_FK FOREIGN KEY (JOB_EXECUTION_ID)
        REFERENCES BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
);

CREATE TABLE IF NOT EXISTS BATCH_STEP_EXECUTION (
    STEP_EXECUTION_ID BIGSERIAL PRIMARY KEY,
    VERSION BIGINT NOT NULL,
    STEP_NAME VARCHAR(100) NOT NULL,
    JOB_EXECUTION_ID BIGINT NOT NULL,
    CREATE_TIME TIMESTAMP NOT NULL,
    START_TIME TIMESTAMP,
    END_TIME TIMESTAMP,
    STATUS VARCHAR(10),
    COMMIT_COUNT BIGINT,
    READ_COUNT BIGINT,
    FILTER_COUNT BIGINT,
    WRITE_COUNT BIGINT,
    READ_SKIP_COUNT BIGINT,
    WRITE_SKIP_COUNT BIGINT,
    PROCESS_SKIP_COUNT BIGINT,
    ROLLBACK_COUNT BIGINT,
    EXIT_CODE VARCHAR(2500),
    EXIT_MESSAGE VARCHAR(2500),
    LAST_UPDATED TIMESTAMP,
    CONSTRAINT JOB_EXEC_STEP_FK FOREIGN KEY (JOB_EXECUTION_ID)
        REFERENCES BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
);

CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION_CONTEXT (
    JOB_EXECUTION_ID BIGINT PRIMARY KEY,
    SHORT_CONTEXT VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT,
    CONSTRAINT JOB_EXEC_CTX_FK FOREIGN KEY (JOB_EXECUTION_ID)
        REFERENCES BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
);

CREATE TABLE IF NOT EXISTS BATCH_STEP_EXECUTION_CONTEXT (
    STEP_EXECUTION_ID BIGINT PRIMARY KEY,
    SHORT_CONTEXT VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT,
    CONSTRAINT STEP_EXEC_CTX_FK FOREIGN KEY (STEP_EXECUTION_ID)
        REFERENCES BATCH_STEP_EXECUTION(STEP_EXECUTION_ID)
);