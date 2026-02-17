-- Complete countries test data with schema and code systems
-- Creates all necessary tables and data for testing countries

-- Create code_system table if it doesn't exist
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

-- Create countries_v table if it doesn't exist
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
    change_request_id UUID,
    is_correction BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    metadata TEXT,
    FOREIGN KEY (code_system_id) REFERENCES code_system(id)
);

-- Clear existing test data
DELETE FROM countries_v WHERE country_code IN ('US', 'CA', 'GB');
DELETE FROM code_system WHERE code = 'ISO3166-1';

-- Insert code system
INSERT INTO code_system (id, code, name, description, owner, created_at, updated_at, is_active)
VALUES (CAST('11111111-1111-1111-1111-111111111111' AS UUID), 'ISO3166-1', 'ISO 3166-1 Country Codes', 'ISO standard for country codes', 'ISO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true);

-- Insert test countries
INSERT INTO countries_v (
    id, version, code_system_id, country_code, country_name, iso2_code, iso3_code, numeric_code,
    is_active, valid_from, valid_to, recorded_at, recorded_by, change_request_id, is_correction, metadata
) VALUES
    (
        CAST('22222222-2222-2222-2222-222222222222' AS UUID),
        1,
        CAST('11111111-1111-1111-1111-111111111111' AS UUID),
        'US',
        'United States',
        'US',
        'USA',
        '840',
        true,
        CURRENT_DATE,
        NULL,
        CURRENT_TIMESTAMP,
        'system',
        CAST('00000000-0000-0000-0000-000000000001' AS UUID),
        false,
        '{"source": "test-seed-data"}'
    ),
    (
        CAST('33333333-3333-3333-3333-333333333333' AS UUID),
        1,
        CAST('11111111-1111-1111-1111-111111111111' AS UUID),
        'CA',
        'Canada',
        'CA',
        'CAN',
        '124',
        true,
        CURRENT_DATE,
        NULL,
        CURRENT_TIMESTAMP,
        'system',
        CAST('00000000-0000-0000-0000-000000000001' AS UUID),
        false,
        '{"source": "test-seed-data"}'
    ),
    (
        CAST('44444444-4444-4444-4444-444444444444' AS UUID),
        1,
        CAST('11111111-1111-1111-1111-111111111111' AS UUID),
        'GB',
        'United Kingdom',
        'GB',
        'GBR',
        '826',
        true,
        CURRENT_DATE,
        NULL,
        CURRENT_TIMESTAMP,
        'system',
        CAST('00000000-0000-0000-0000-000000000001' AS UUID),
        false,
        '{"source": "test-seed-data"}'
    );