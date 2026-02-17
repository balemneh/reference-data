-- Complete ports test data with schema and code systems
-- Creates all necessary tables and data for testing ports

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

-- Create ports_v table if it doesn't exist
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
    change_request_id UUID,
    is_correction BOOLEAN DEFAULT FALSE,
    metadata TEXT,
    FOREIGN KEY (code_system_id) REFERENCES code_system(id)
);

-- Clear existing test data
DELETE FROM ports_v WHERE port_code IN ('USLAX', 'USNYC', 'GBLON');
DELETE FROM code_system WHERE code = 'UN-LOCODE';

-- Insert code system
INSERT INTO code_system (id, code, name, description, owner, created_at, updated_at, is_active)
VALUES (CAST('f6f6f6f6-f6f6-f6f6-f6f6-f6f6f6f6f6f6' AS UUID), 'UN-LOCODE', 'UN/LOCODE Port Codes', 'United Nations Code for Trade and Transport Locations', 'UN/ECE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true);

-- Insert test ports
INSERT INTO ports_v (
    id, version, code_system_id, port_code, port_name, city, state_province, country_code,
    latitude, longitude, port_type, un_locode, cbp_port_code, timezone, is_active, valid_from, valid_to,
    recorded_at, recorded_by, change_request_id, is_correction, metadata
) VALUES
    (
        CAST('71717171-7171-7171-7171-717171717171' AS UUID),
        1,
        CAST('f6f6f6f6-f6f6-f6f6-f6f6-f6f6f6f6f6f6' AS UUID),
        'USLAX',
        'Los Angeles',
        'Los Angeles',
        'CA',
        'USA',
        33.7373000,
        -118.2644000,
        'Seaport',
        'LAX',
        '2704',
        'America/Los_Angeles',
        true,
        CURRENT_DATE,
        NULL,
        CURRENT_TIMESTAMP,
        'system',
        CAST('00000000-0000-0000-0000-000000000003' AS UUID),
        false,
        '{"source": "test-seed-data"}'
    ),
    (
        CAST('81818181-8181-8181-8181-818181818181' AS UUID),
        1,
        CAST('f6f6f6f6-f6f6-f6f6-f6f6-f6f6f6f6f6f6' AS UUID),
        'USNYC',
        'New York',
        'New York',
        'NY',
        'USA',
        40.6892000,
        -74.0445000,
        'Seaport',
        'NYC',
        '1001',
        'America/New_York',
        true,
        CURRENT_DATE,
        NULL,
        CURRENT_TIMESTAMP,
        'system',
        CAST('00000000-0000-0000-0000-000000000003' AS UUID),
        false,
        '{"source": "test-seed-data"}'
    ),
    (
        CAST('91919191-9191-9191-9191-919191919191' AS UUID),
        1,
        CAST('f6f6f6f6-f6f6-f6f6-f6f6-f6f6f6f6f6f6' AS UUID),
        'GBLON',
        'London',
        'London',
        NULL,
        'GBR',
        51.5074000,
        -0.1278000,
        'Seaport',
        'LON',
        NULL,
        'Europe/London',
        true,
        CURRENT_DATE,
        NULL,
        CURRENT_TIMESTAMP,
        'system',
        CAST('00000000-0000-0000-0000-000000000003' AS UUID),
        false,
        '{"source": "test-seed-data"}'
    );