-- Test seed data for ports (H2 compatible)
-- This script provides minimal test data for unit tests

-- Ensure code systems exist first (H2 compatible)
INSERT INTO code_system (id, code, name, description, owner, created_at, updated_at, is_active)
SELECT * FROM (VALUES (CAST('ffffffff-ffff-ffff-ffff-ffffffffffff' AS UUID), 'UN-LOCODE', 'UN/LOCODE Port Codes', 'United Nations Code for Trade and Transport Locations', 'UN/ECE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true)) AS t(id, code, name, description, owner, created_at, updated_at, is_active)
WHERE NOT EXISTS (SELECT 1 FROM code_system WHERE code = 'UN-LOCODE');

-- Insert test ports (H2 compatible - match the actual port entity fields)
INSERT INTO ports_v (
    id, version, code_system_id, port_code, port_name, city, state_code, country_code,
    latitude, longitude, port_type, is_active, valid_from, valid_to,
    recorded_at, recorded_by, change_request_id, is_correction, metadata
) VALUES
    (
        CAST('gggggggg-gggg-gggg-gggg-gggggggggggg' AS UUID),
        1,
        CAST('ffffffff-ffff-ffff-ffff-ffffffffffff' AS UUID),
        'USLAX',
        'Los Angeles',
        'Los Angeles',
        'CA',
        'US',
        33.7373000,
        -118.2644000,
        'Seaport',
        true,
        CURRENT_DATE,
        NULL,
        CURRENT_TIMESTAMP,
        'system',
        'SEED-003',
        false,
        '{"source": "test-seed-data"}'
    ),
    (
        CAST('hhhhhhhh-hhhh-hhhh-hhhh-hhhhhhhhhhhh' AS UUID),
        1,
        CAST('ffffffff-ffff-ffff-ffff-ffffffffffff' AS UUID),
        'USNYC',
        'New York',
        'New York',
        'NY',
        'US',
        40.6892000,
        -74.0445000,
        'Seaport',
        true,
        CURRENT_DATE,
        NULL,
        CURRENT_TIMESTAMP,
        'system',
        'SEED-003',
        false,
        '{"source": "test-seed-data"}'
    ),
    (
        CAST('iiiiiiii-iiii-iiii-iiii-iiiiiiiiiiii' AS UUID),
        1,
        CAST('ffffffff-ffff-ffff-ffff-ffffffffffff' AS UUID),
        'GBLON',
        'London',
        'London',
        NULL,
        'GB',
        51.5074000,
        -0.1278000,
        'Seaport',
        true,
        CURRENT_DATE,
        NULL,
        CURRENT_TIMESTAMP,
        'system',
        'SEED-003',
        false,
        '{"source": "test-seed-data"}'
    );