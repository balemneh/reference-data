-- Test database schema initialization script
-- This script sets up the minimal schema needed for integration tests

CREATE SCHEMA IF NOT EXISTS reference_data;

-- Code systems table
CREATE TABLE reference_data.code_system (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Countries bitemporal table
CREATE TABLE reference_data.countries_v (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    code_system_id UUID NOT NULL,
    country_code VARCHAR(10) NOT NULL,
    country_name VARCHAR(255) NOT NULL,
    iso2_code VARCHAR(2),
    iso3_code VARCHAR(3),
    numeric_code VARCHAR(3),
    valid_from DATE NOT NULL,
    valid_to DATE,
    recorded_at TIMESTAMP NOT NULL,
    recorded_by VARCHAR(100) NOT NULL,
    change_request_id VARCHAR(100),
    is_correction BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    metadata JSONB,
    FOREIGN KEY (code_system_id) REFERENCES reference_data.code_system(id)
);

-- Create indexes for better query performance
CREATE INDEX idx_countries_v_lookup ON reference_data.countries_v(country_code, code_system_id, valid_from);
CREATE INDEX idx_countries_v_active ON reference_data.countries_v(is_active, valid_to);
CREATE INDEX idx_countries_v_valid_dates ON reference_data.countries_v(valid_from, valid_to);

-- Current countries view
CREATE VIEW reference_data.countries_current AS
SELECT DISTINCT ON (country_code, code_system_id)
    id,
    version,
    code_system_id,
    country_code,
    country_name,
    iso2_code,
    iso3_code,
    numeric_code,
    valid_from,
    valid_to,
    recorded_at,
    recorded_by,
    change_request_id,
    is_correction,
    is_active,
    metadata
FROM reference_data.countries_v
WHERE valid_to IS NULL OR valid_to > CURRENT_DATE
ORDER BY country_code, code_system_id, version DESC;

-- Ports bitemporal table  
CREATE TABLE reference_data.ports_v (
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
    metadata JSONB,
    FOREIGN KEY (code_system_id) REFERENCES reference_data.code_system(id)
);

-- Airports bitemporal table
CREATE TABLE reference_data.airports_v (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    iata_code VARCHAR(3),
    icao_code VARCHAR(4),
    airport_name VARCHAR(255) NOT NULL,
    city VARCHAR(100),
    state_province VARCHAR(100),
    country_code VARCHAR(3) NOT NULL,
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    elevation_ft INTEGER,
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
    metadata JSONB
);

-- Carriers bitemporal table
CREATE TABLE reference_data.carriers_v (
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
    metadata JSONB,
    FOREIGN KEY (code_system_id) REFERENCES reference_data.code_system(id)
);

-- Code mapping table
CREATE TABLE reference_data.code_mapping (
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
    metadata JSONB,
    FOREIGN KEY (from_system_id) REFERENCES reference_data.code_system(id),
    FOREIGN KEY (to_system_id) REFERENCES reference_data.code_system(id)
);

-- Outbox events table
CREATE TABLE reference_data.outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PENDING' NOT NULL
);

CREATE INDEX idx_outbox_events_status ON reference_data.outbox_events(status, created_at);

-- Insert test code systems
INSERT INTO reference_data.code_system (id, code, name, description, owner, created_at, updated_at) VALUES
    (gen_random_uuid(), 'ISO3166-1', 'ISO 3166-1 Country Codes', 'International standard for country codes', 'ISO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'CBP-COUNTRY5', 'CBP 5-Character Country Codes', 'CBP internal country code system', 'CBP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'UN-LOCODE', 'UN Location Codes', 'United Nations Location Codes for ports and airports', 'UN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);