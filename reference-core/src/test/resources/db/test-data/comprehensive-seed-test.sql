-- Comprehensive test seed data for all entities (H2 compatible)
-- This script provides test data for comprehensive integration tests

-- Insert code systems first (avoiding duplicates)
INSERT INTO code_system (id, code, name, description, owner, created_at, updated_at, is_active)
SELECT * FROM (VALUES
    (CAST('11111111-1111-1111-1111-111111111111' AS UUID), 'ISO3166-1', 'ISO 3166-1 Country Codes', 'ISO standard for country codes', 'ISO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true),
    (CAST('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa' AS UUID), 'IATA', 'IATA Airport Codes', 'International Air Transport Association airport codes', 'IATA', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true),
    (CAST('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb' AS UUID), 'ICAO', 'ICAO Airport Codes', 'International Civil Aviation Organization airport codes', 'ICAO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true),
    (CAST('ffffffff-ffff-ffff-ffff-ffffffffffff' AS UUID), 'UN-LOCODE', 'UN/LOCODE Port Codes', 'United Nations Code for Trade and Transport Locations', 'UN/ECE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true)
) AS t(id, code, name, description, owner, created_at, updated_at, is_active)
WHERE NOT EXISTS (SELECT 1 FROM code_system WHERE code = t.code);

-- Insert test countries (individual statements to avoid conflicts)
INSERT INTO countries_v (id, version, code_system_id, country_code, country_name, iso2_code, iso3_code, numeric_code, is_active, valid_from, valid_to, recorded_at, recorded_by, change_request_id, is_correction, metadata)
SELECT CAST('22222222-2222-2222-2222-222222222222' AS UUID), 1, CAST('11111111-1111-1111-1111-111111111111' AS UUID), 'US', 'United States', 'US', 'USA', '840', true, CURRENT_DATE, NULL, CURRENT_TIMESTAMP, 'system', 'SEED-001', false, '{"source": "test-seed-data"}'
WHERE NOT EXISTS (SELECT 1 FROM countries_v WHERE country_code = 'US' AND code_system_id = CAST('11111111-1111-1111-1111-111111111111' AS UUID));

INSERT INTO countries_v (id, version, code_system_id, country_code, country_name, iso2_code, iso3_code, numeric_code, is_active, valid_from, valid_to, recorded_at, recorded_by, change_request_id, is_correction, metadata)
SELECT CAST('33333333-3333-3333-3333-333333333333' AS UUID), 1, CAST('11111111-1111-1111-1111-111111111111' AS UUID), 'CA', 'Canada', 'CA', 'CAN', '124', true, CURRENT_DATE, NULL, CURRENT_TIMESTAMP, 'system', 'SEED-001', false, '{"source": "test-seed-data"}'
WHERE NOT EXISTS (SELECT 1 FROM countries_v WHERE country_code = 'CA' AND code_system_id = CAST('11111111-1111-1111-1111-111111111111' AS UUID));

INSERT INTO countries_v (id, version, code_system_id, country_code, country_name, iso2_code, iso3_code, numeric_code, is_active, valid_from, valid_to, recorded_at, recorded_by, change_request_id, is_correction, metadata)
SELECT CAST('44444444-4444-4444-4444-444444444444' AS UUID), 1, CAST('11111111-1111-1111-1111-111111111111' AS UUID), 'GB', 'United Kingdom', 'GB', 'GBR', '826', true, CURRENT_DATE, NULL, CURRENT_TIMESTAMP, 'system', 'SEED-001', false, '{"source": "test-seed-data"}'
WHERE NOT EXISTS (SELECT 1 FROM countries_v WHERE country_code = 'GB' AND code_system_id = CAST('11111111-1111-1111-1111-111111111111' AS UUID));

-- Insert test airports (individual statements to avoid conflicts)
INSERT INTO airports_v (id, version, code_system_id, airport_name, iata_code, icao_code, city, country_code, is_active, valid_from, valid_to, recorded_at, recorded_by, change_request_id, is_correction, metadata)
SELECT CAST('cccccccc-cccc-cccc-cccc-cccccccccccc' AS UUID), 1, CAST('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa' AS UUID), 'Los Angeles International Airport', 'LAX', 'KLAX', 'Los Angeles', 'USA', true, CURRENT_DATE, NULL, CURRENT_TIMESTAMP, 'system', 'SEED-002', false, '{"source": "test-seed-data"}'
WHERE NOT EXISTS (SELECT 1 FROM airports_v WHERE iata_code = 'LAX' AND code_system_id = CAST('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa' AS UUID));

INSERT INTO airports_v (id, version, code_system_id, airport_name, iata_code, icao_code, city, country_code, is_active, valid_from, valid_to, recorded_at, recorded_by, change_request_id, is_correction, metadata)
SELECT CAST('dddddddd-dddd-dddd-dddd-dddddddddddd' AS UUID), 1, CAST('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa' AS UUID), 'London Heathrow Airport', 'LHR', 'EGLL', 'London', 'GBR', true, CURRENT_DATE, NULL, CURRENT_TIMESTAMP, 'system', 'SEED-002', false, '{"source": "test-seed-data"}'
WHERE NOT EXISTS (SELECT 1 FROM airports_v WHERE iata_code = 'LHR' AND code_system_id = CAST('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa' AS UUID));

INSERT INTO airports_v (id, version, code_system_id, airport_name, iata_code, icao_code, city, country_code, is_active, valid_from, valid_to, recorded_at, recorded_by, change_request_id, is_correction, metadata)
SELECT CAST('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee' AS UUID), 1, CAST('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa' AS UUID), 'John F. Kennedy International Airport', 'JFK', 'KJFK', 'New York', 'USA', true, CURRENT_DATE, NULL, CURRENT_TIMESTAMP, 'system', 'SEED-002', false, '{"source": "test-seed-data"}'
WHERE NOT EXISTS (SELECT 1 FROM airports_v WHERE iata_code = 'JFK' AND code_system_id = CAST('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa' AS UUID));

-- Insert test ports (individual statements to avoid conflicts)
INSERT INTO ports_v (id, version, code_system_id, port_code, port_name, city, state_code, country_code, latitude, longitude, port_type, is_active, valid_from, valid_to, recorded_at, recorded_by, change_request_id, is_correction, metadata)
SELECT CAST('gggggggg-gggg-gggg-gggg-gggggggggggg' AS UUID), 1, CAST('ffffffff-ffff-ffff-ffff-ffffffffffff' AS UUID), 'USLAX', 'Los Angeles', 'Los Angeles', 'CA', 'US', 33.7373000, -118.2644000, 'Seaport', true, CURRENT_DATE, NULL, CURRENT_TIMESTAMP, 'system', 'SEED-003', false, '{"source": "test-seed-data"}'
WHERE NOT EXISTS (SELECT 1 FROM ports_v WHERE port_code = 'USLAX' AND code_system_id = CAST('ffffffff-ffff-ffff-ffff-ffffffffffff' AS UUID));

INSERT INTO ports_v (id, version, code_system_id, port_code, port_name, city, state_code, country_code, latitude, longitude, port_type, is_active, valid_from, valid_to, recorded_at, recorded_by, change_request_id, is_correction, metadata)
SELECT CAST('hhhhhhhh-hhhh-hhhh-hhhh-hhhhhhhhhhhh' AS UUID), 1, CAST('ffffffff-ffff-ffff-ffff-ffffffffffff' AS UUID), 'USNYC', 'New York', 'New York', 'NY', 'US', 40.6892000, -74.0445000, 'Seaport', true, CURRENT_DATE, NULL, CURRENT_TIMESTAMP, 'system', 'SEED-003', false, '{"source": "test-seed-data"}'
WHERE NOT EXISTS (SELECT 1 FROM ports_v WHERE port_code = 'USNYC' AND code_system_id = CAST('ffffffff-ffff-ffff-ffff-ffffffffffff' AS UUID));

INSERT INTO ports_v (id, version, code_system_id, port_code, port_name, city, state_code, country_code, latitude, longitude, port_type, is_active, valid_from, valid_to, recorded_at, recorded_by, change_request_id, is_correction, metadata)
SELECT CAST('iiiiiiii-iiii-iiii-iiii-iiiiiiiiiiii' AS UUID), 1, CAST('ffffffff-ffff-ffff-ffff-ffffffffffff' AS UUID), 'GBLON', 'London', 'London', NULL, 'GB', 51.5074000, -0.1278000, 'Seaport', true, CURRENT_DATE, NULL, CURRENT_TIMESTAMP, 'system', 'SEED-003', false, '{"source": "test-seed-data"}'
WHERE NOT EXISTS (SELECT 1 FROM ports_v WHERE port_code = 'GBLON' AND code_system_id = CAST('ffffffff-ffff-ffff-ffff-ffffffffffff' AS UUID));