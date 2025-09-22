-- Base code systems used by all tests (H2 compatible)
-- This file should be loaded first to establish code systems

-- Clear and insert fresh code systems for testing
DELETE FROM code_system WHERE code IN ('ISO3166-1', 'IATA', 'ICAO', 'UN-LOCODE');

INSERT INTO code_system (id, code, name, description, owner, created_at, updated_at, is_active)
VALUES
    (CAST('11111111-1111-1111-1111-111111111111' AS UUID), 'ISO3166-1', 'ISO 3166-1 Country Codes', 'ISO standard for country codes', 'ISO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true),
    (CAST('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1' AS UUID), 'IATA', 'IATA Airport Codes', 'International Air Transport Association airport codes', 'IATA', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true),
    (CAST('b2b2b2b2-b2b2-b2b2-b2b2-b2b2b2b2b2b2' AS UUID), 'ICAO', 'ICAO Airport Codes', 'International Civil Aviation Organization airport codes', 'ICAO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true),
    (CAST('f6f6f6f6-f6f6-f6f6-f6f6-f6f6f6f6f6f6' AS UUID), 'UN-LOCODE', 'UN/LOCODE Port Codes', 'United Nations Code for Trade and Transport Locations', 'UN/ECE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true);