-- Comprehensive test seed data for all entities (H2 compatible)
-- This script provides test data for comprehensive integration tests

-- Insert code systems first (H2 compatible syntax)
MERGE INTO code_system (id, code, name, description, owner, created_at, updated_at, is_active)
KEY (code)
VALUES
    (CAST('11111111-1111-1111-1111-111111111111' AS UUID), 'ISO3166-1', 'ISO 3166-1 Country Codes', 'ISO standard for country codes', 'ISO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true),
    (CAST('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1' AS UUID), 'IATA', 'IATA Airport Codes', 'International Air Transport Association airport codes', 'IATA', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true),
    (CAST('b2b2b2b2-b2b2-b2b2-b2b2-b2b2b2b2b2b2' AS UUID), 'ICAO', 'ICAO Airport Codes', 'International Civil Aviation Organization airport codes', 'ICAO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true),
    (CAST('f6f6f6f6-f6f6-f6f6-f6f6-f6f6f6f6f6f6' AS UUID), 'UN-LOCODE', 'UN/LOCODE Port Codes', 'United Nations Code for Trade and Transport Locations', 'UN/ECE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true);

-- Insert test countries (H2 compatible MERGE statements)
MERGE INTO countries_v (id, version, code_system_id, country_code, country_name, iso2_code, iso3_code, numeric_code, is_active, valid_from, valid_to, recorded_at, recorded_by, change_request_id, is_correction, metadata)
KEY (id)
VALUES
    (CAST('22222222-2222-2222-2222-222222222222' AS UUID), 1, CAST('11111111-1111-1111-1111-111111111111' AS UUID), 'US', 'United States', 'US', 'USA', '840', true, CURRENT_DATE, NULL, CURRENT_TIMESTAMP, 'system', NULL, false, '{"source": "test-seed-data"}'),
    (CAST('33333333-3333-3333-3333-333333333333' AS UUID), 1, CAST('11111111-1111-1111-1111-111111111111' AS UUID), 'CA', 'Canada', 'CA', 'CAN', '124', true, CURRENT_DATE, NULL, CURRENT_TIMESTAMP, 'system', NULL, false, '{"source": "test-seed-data"}'),
    (CAST('44444444-4444-4444-4444-444444444444' AS UUID), 1, CAST('11111111-1111-1111-1111-111111111111' AS UUID), 'GB', 'United Kingdom', 'GB', 'GBR', '826', true, CURRENT_DATE, NULL, CURRENT_TIMESTAMP, 'system', NULL, false, '{"source": "test-seed-data"}');


-- Insert test ports (H2 compatible MERGE statements)
MERGE INTO ports_v (id, version, code_system_id, port_code, port_name, city, state_province, country_code, latitude, longitude, port_type, un_locode, cbp_port_code, timezone, is_active, valid_from, valid_to, recorded_at, recorded_by, change_request_id, is_correction, metadata)
KEY (id)
VALUES
    (CAST('71717171-7171-7171-7171-717171717171' AS UUID), 1, CAST('f6f6f6f6-f6f6-f6f6-f6f6-f6f6f6f6f6f6' AS UUID), 'USLAX', 'Los Angeles', 'Los Angeles', 'CA', 'USA', 33.7373000, -118.2644000, 'Seaport', 'LAX', '2704', 'America/Los_Angeles', true, CURRENT_DATE, NULL, CURRENT_TIMESTAMP, 'system', NULL, false, '{"source": "test-seed-data"}'),
    (CAST('81818181-8181-8181-8181-818181818181' AS UUID), 1, CAST('f6f6f6f6-f6f6-f6f6-f6f6-f6f6f6f6f6f6' AS UUID), 'USNYC', 'New York', 'New York', 'NY', 'USA', 40.6892000, -74.0445000, 'Seaport', 'NYC', '1001', 'America/New_York', true, CURRENT_DATE, NULL, CURRENT_TIMESTAMP, 'system', NULL, false, '{"source": "test-seed-data"}'),
    (CAST('91919191-9191-9191-9191-919191919191' AS UUID), 1, CAST('f6f6f6f6-f6f6-f6f6-f6f6-f6f6f6f6f6f6' AS UUID), 'GBLON', 'London', 'London', NULL, 'GBR', 51.5074000, -0.1278000, 'Seaport', 'LON', NULL, 'Europe/London', true, CURRENT_DATE, NULL, CURRENT_TIMESTAMP, 'system', NULL, false, '{"source": "test-seed-data"}');

-- Insert test airports (H2 compatible MERGE statements)
MERGE INTO airports_v (id, version, code_system_id, airport_name, iata_code, icao_code, city, state_province, country_code, latitude, longitude, elevation, airport_type, timezone, is_active, valid_from, valid_to, recorded_at, recorded_by, change_request_id, is_correction, metadata)
KEY (id)
VALUES
    (CAST('c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3' AS UUID), 1, CAST('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1' AS UUID), 'Los Angeles International Airport', 'LAX', 'KLAX', 'Los Angeles', 'California', 'USA', 33.9425, -118.4081, 125, 'Large Hub', 'America/Los_Angeles', true, CURRENT_DATE, NULL, CURRENT_TIMESTAMP, 'system', NULL, false, '{"source": "test-seed-data"}'),
    (CAST('d4d4d4d4-d4d4-d4d4-d4d4-d4d4d4d4d4d4' AS UUID), 1, CAST('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1' AS UUID), 'London Heathrow Airport', 'LHR', 'EGLL', 'London', NULL, 'GBR', 51.4700, -0.4543, 83, 'Large Hub', 'Europe/London', true, CURRENT_DATE, NULL, CURRENT_TIMESTAMP, 'system', NULL, false, '{"source": "test-seed-data"}'),
    (CAST('e5e5e5e5-e5e5-e5e5-e5e5-e5e5e5e5e5e5' AS UUID), 1, CAST('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1' AS UUID), 'John F. Kennedy International Airport', 'JFK', 'KJFK', 'New York', 'New York', 'USA', 40.6413, -73.7781, 13, 'Large Hub', 'America/New_York', true, CURRENT_DATE, NULL, CURRENT_TIMESTAMP, 'system', NULL, false, '{"source": "test-seed-data"}');
