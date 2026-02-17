-- H2 database schema initialization script for tests
-- This script creates tables without schema prefixes to match Hibernate expectations in test mode

-- Code systems table (no schema prefix for H2 tests)
CREATE TABLE IF NOT EXISTS code_system (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

-- Countries bitemporal table (no schema prefix for H2 tests)
CREATE TABLE IF NOT EXISTS countries_v (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    code_system_id UUID NOT NULL,
    country_code VARCHAR(10) NOT NULL,
    country_name VARCHAR(255) NOT NULL,
    iso2_code VARCHAR(2),
    iso3_code VARCHAR(3),
    numeric_code VARCHAR(3),
    alpha2_code VARCHAR(2),
    alpha3_code VARCHAR(3), 
    valid_from DATE NOT NULL,
    valid_to DATE,
    recorded_at TIMESTAMP NOT NULL,
    recorded_by VARCHAR(100) NOT NULL,
    change_request_id VARCHAR(100),
    is_correction BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    metadata TEXT, -- H2 doesn't support JSONB, use TEXT
    FOREIGN KEY (code_system_id) REFERENCES code_system(id)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_countries_v_lookup ON countries_v(country_code, code_system_id, valid_from);
CREATE INDEX IF NOT EXISTS idx_countries_v_active ON countries_v(is_active, valid_to);
CREATE INDEX IF NOT EXISTS idx_countries_v_valid_dates ON countries_v(valid_from, valid_to);

-- Ports bitemporal table (no schema prefix for H2 tests)
CREATE TABLE IF NOT EXISTS ports_v (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    code_system_id UUID NOT NULL,
    port_code VARCHAR(10) NOT NULL,
    port_name VARCHAR(255) NOT NULL,
    city VARCHAR(100),
    state_province VARCHAR(100),
    country_code VARCHAR(3) NOT NULL,
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    port_type VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    un_locode VARCHAR(10),
    cbp_port_code VARCHAR(10),
    timezone VARCHAR(50),
    valid_from DATE NOT NULL,
    valid_to DATE,
    recorded_at TIMESTAMP NOT NULL,
    recorded_by VARCHAR(100) NOT NULL,
    change_request_id VARCHAR(100),
    is_correction BOOLEAN DEFAULT FALSE,
    metadata TEXT,
    FOREIGN KEY (code_system_id) REFERENCES code_system(id)
);

-- Airports bitemporal table (no schema prefix for H2 tests)
CREATE TABLE IF NOT EXISTS airports_v (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    code_system_id UUID NOT NULL,
    iata_code VARCHAR(3),
    icao_code VARCHAR(4),
    airport_name VARCHAR(255) NOT NULL,
    city VARCHAR(100),
    state_province VARCHAR(100),
    country_code VARCHAR(3) NOT NULL,
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    elevation INTEGER,
    airport_type VARCHAR(50),
    is_international BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    timezone VARCHAR(50),
    cbp_airport_code VARCHAR(10),
    has_customs BOOLEAN DEFAULT FALSE,
    has_immigration BOOLEAN DEFAULT FALSE,
    valid_from DATE NOT NULL,
    valid_to DATE,
    recorded_at TIMESTAMP NOT NULL,
    recorded_by VARCHAR(100) NOT NULL,
    change_request_id VARCHAR(100),
    is_correction BOOLEAN DEFAULT FALSE,
    metadata TEXT,
    FOREIGN KEY (code_system_id) REFERENCES code_system(id)
);

-- Carriers bitemporal table (no schema prefix for H2 tests)
CREATE TABLE IF NOT EXISTS carriers_v (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    code_system_id UUID NOT NULL,
    carrier_code VARCHAR(10) NOT NULL,
    carrier_name VARCHAR(255) NOT NULL,
    iata_code VARCHAR(2),
    icao_code VARCHAR(3),
    country_code VARCHAR(3),
    carrier_type VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    operating_name VARCHAR(100),
    scac_code VARCHAR(20),
    dot_number VARCHAR(20),
    mc_number VARCHAR(20),
    is_passenger_carrier BOOLEAN DEFAULT FALSE,
    is_cargo_carrier BOOLEAN DEFAULT FALSE,
    alliance VARCHAR(50),
    valid_from DATE NOT NULL,
    valid_to DATE,
    recorded_at TIMESTAMP NOT NULL,
    recorded_by VARCHAR(100) NOT NULL,
    change_request_id VARCHAR(100),
    is_correction BOOLEAN DEFAULT FALSE,
    metadata TEXT,
    FOREIGN KEY (code_system_id) REFERENCES code_system(id)
);

-- Code mapping table (no schema prefix for H2 tests)
CREATE TABLE IF NOT EXISTS code_mapping (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    from_system_id UUID NOT NULL,
    from_code VARCHAR(50) NOT NULL,
    to_system_id UUID NOT NULL,
    to_code VARCHAR(50) NOT NULL,
    rule_id VARCHAR(100),
    confidence DECIMAL(5,2) DEFAULT 100.00,
    mapping_type VARCHAR(50),
    is_deprecated BOOLEAN DEFAULT FALSE,
    deprecation_reason TEXT,
    valid_from DATE NOT NULL,
    valid_to DATE,
    recorded_at TIMESTAMP NOT NULL,
    recorded_by VARCHAR(100) NOT NULL,
    change_request_id VARCHAR(100),
    is_correction BOOLEAN DEFAULT FALSE,
    metadata TEXT,
    FOREIGN KEY (from_system_id) REFERENCES code_system(id),
    FOREIGN KEY (to_system_id) REFERENCES code_system(id)
);

-- Outbox events table (no schema prefix for H2 tests)
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL, -- Use TEXT instead of JSONB for H2
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PENDING' NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_status ON outbox_events(status, created_at);

-- Change requests table (no schema prefix for H2 tests)
CREATE TABLE IF NOT EXISTS change_requests (
    id UUID PRIMARY KEY,
    cr_number VARCHAR(50) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    requester_id VARCHAR(100) NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    priority VARCHAR(20),
    approval_notes TEXT,
    rejection_reason TEXT,
    approved_by VARCHAR(100),
    approved_at TIMESTAMP,
    rejected_by VARCHAR(100),
    rejected_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    scheduled_at TIMESTAMP,
    completed_at TIMESTAMP,
    metadata TEXT
);

-- Bulk import batches table (no schema prefix for H2 tests)
CREATE TABLE IF NOT EXISTS bulk_import_batches (
    id UUID PRIMARY KEY,
    batch_name VARCHAR(255) NOT NULL,
    change_request_id UUID NOT NULL,
    source_system VARCHAR(100) NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_records INTEGER,
    processed_records INTEGER,
    failed_records INTEGER,
    error_summary TEXT,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    completed_at TIMESTAMP,
    metadata TEXT,
    FOREIGN KEY (change_request_id) REFERENCES change_requests(id)
);

-- Bulk import staging table (no schema prefix for H2 tests)
CREATE TABLE IF NOT EXISTS bulk_import_staging (
    id UUID PRIMARY KEY,
    import_batch_id UUID NOT NULL,
    change_request_id UUID NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    source_system VARCHAR(100) NOT NULL,
    row_number INTEGER NOT NULL,
    natural_key VARCHAR(255) NOT NULL,
    raw_data TEXT NOT NULL,
    target_table VARCHAR(100) NOT NULL,
    validation_status VARCHAR(50),
    validation_errors TEXT,
    processing_status VARCHAR(50),
    processing_errors TEXT,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    metadata TEXT,
    FOREIGN KEY (import_batch_id) REFERENCES bulk_import_batches(id),
    FOREIGN KEY (change_request_id) REFERENCES change_requests(id)
);

-- Audit log table (no schema prefix for H2 tests)
CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100),
    operation_type VARCHAR(50) NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    event_timestamp TIMESTAMP NOT NULL,
    change_request_id VARCHAR(100),
    description TEXT,
    old_value TEXT,
    new_value TEXT,
    status VARCHAR(50),
    metadata TEXT,
    FOREIGN KEY (change_request_id) REFERENCES change_requests(id)
);

CREATE INDEX IF NOT EXISTS idx_change_requests_status ON change_requests(status);
CREATE INDEX IF NOT EXISTS idx_change_requests_requester ON change_requests(requester_id);
CREATE INDEX IF NOT EXISTS idx_bulk_import_batches_status ON bulk_import_batches(status);
CREATE INDEX IF NOT EXISTS idx_bulk_import_staging_batch ON bulk_import_staging(import_batch_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_change_request ON audit_log(change_request_id);