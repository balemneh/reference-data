-- Test seed data for airports (H2 compatible)
-- This script provides minimal test data for unit tests

-- Ensure code systems exist first (H2 compatible)
INSERT INTO code_system (id, code, name, description, owner, created_at, updated_at, is_active)
SELECT * FROM (VALUES (CAST('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa' AS UUID), 'IATA', 'IATA Airport Codes', 'International Air Transport Association airport codes', 'IATA', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true)) AS t(id, code, name, description, owner, created_at, updated_at, is_active)
WHERE NOT EXISTS (SELECT 1 FROM code_system WHERE code = 'IATA');

INSERT INTO code_system (id, code, name, description, owner, created_at, updated_at, is_active)
SELECT * FROM (VALUES (CAST('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb' AS UUID), 'ICAO', 'ICAO Airport Codes', 'International Civil Aviation Organization airport codes', 'ICAO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true)) AS t(id, code, name, description, owner, created_at, updated_at, is_active)
WHERE NOT EXISTS (SELECT 1 FROM code_system WHERE code = 'ICAO');

-- Insert test airports (H2 compatible - match the actual airport entity fields)
INSERT INTO airports_v (
    id, version, code_system_id, airport_name, iata_code, icao_code, city, country_code,
    is_active, valid_from, valid_to, recorded_at, recorded_by, change_request_id, is_correction, metadata
) VALUES
    (
        CAST('cccccccc-cccc-cccc-cccc-cccccccccccc' AS UUID),
        1,
        CAST('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa' AS UUID),
        'Los Angeles International Airport',
        'LAX',
        'KLAX',
        'Los Angeles',
        'USA',
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
        CAST('dddddddd-dddd-dddd-dddd-dddddddddddd' AS UUID),
        1,
        CAST('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa' AS UUID),
        'London Heathrow Airport',
        'LHR',
        'EGLL',
        'London',
        'GBR',
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
        CAST('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee' AS UUID),
        1,
        CAST('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa' AS UUID),
        'John F. Kennedy International Airport',
        'JFK',
        'KJFK',
        'New York',
        'USA',
        true,
        CURRENT_DATE,
        NULL,
        CURRENT_TIMESTAMP,
        'system',
        'SEED-002',
        false,
        '{"source": "test-seed-data"}'
    );