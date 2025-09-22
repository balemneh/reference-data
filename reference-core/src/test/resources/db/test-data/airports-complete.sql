-- Complete airports test data with schema and code systems
-- Creates all necessary tables and data for testing airports

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

-- Create airports_v table if it doesn't exist
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

-- Clear existing test data
DELETE FROM airports_v WHERE iata_code IN ('LAX', 'LHR', 'JFK');
DELETE FROM code_system WHERE code IN ('IATA', 'ICAO');

-- Insert code systems
INSERT INTO code_system (id, code, name, description, owner, created_at, updated_at, is_active)
VALUES
    (CAST('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1' AS UUID), 'IATA', 'IATA Airport Codes', 'International Air Transport Association airport codes', 'IATA', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true),
    (CAST('b2b2b2b2-b2b2-b2b2-b2b2-b2b2b2b2b2b2' AS UUID), 'ICAO', 'ICAO Airport Codes', 'International Civil Aviation Organization airport codes', 'ICAO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true);

-- Insert test airports
INSERT INTO airports_v (
    id, version, code_system_id, airport_name, iata_code, icao_code, city, state_province, country_code,
    latitude, longitude, elevation, airport_type, timezone,
    is_active, valid_from, valid_to, recorded_at, recorded_by, change_request_id, is_correction, metadata
) VALUES
    (
        CAST('c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3' AS UUID),
        1,
        CAST('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1' AS UUID),
        'Los Angeles International Airport',
        'LAX',
        'KLAX',
        'Los Angeles',
        'California',
        'USA',
        33.9425,
        -118.4081,
        125,
        'Large Hub',
        'America/Los_Angeles',
        true,
        CURRENT_DATE,
        NULL,
        CURRENT_TIMESTAMP,
        'system',
        'SEED-002',
        false,
        '{"source": "test-seed-data"}'
    ),
    (
        CAST('d4d4d4d4-d4d4-d4d4-d4d4-d4d4d4d4d4d4' AS UUID),
        1,
        CAST('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1' AS UUID),
        'London Heathrow Airport',
        'LHR',
        'EGLL',
        'London',
        NULL,
        'GBR',
        51.4700,
        -0.4543,
        83,
        'International',
        'Europe/London',
        true,
        CURRENT_DATE,
        NULL,
        CURRENT_TIMESTAMP,
        'system',
        'SEED-002',
        false,
        '{"source": "test-seed-data"}'
    ),
    (
        CAST('e5e5e5e5-e5e5-e5e5-e5e5-e5e5e5e5e5e5' AS UUID),
        1,
        CAST('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1' AS UUID),
        'John F. Kennedy International Airport',
        'JFK',
        'KJFK',
        'New York',
        'New York',
        'USA',
        40.6413,
        -73.7781,
        13,
        'Large Hub',
        'America/New_York',
        true,
        CURRENT_DATE,
        NULL,
        CURRENT_TIMESTAMP,
        'system',
        'SEED-002',
        false,
        '{"source": "test-seed-data"}'
    );