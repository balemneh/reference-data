-- Test seed data for countries
-- This script provides minimal test data for unit tests (H2 compatible)
-- NOTE: Requires code-systems-base.sql to be loaded first

-- Insert test countries using proper UUID casting for H2
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
        'SEED-001',
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
        'SEED-001',
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
        'SEED-001',
        false,
        '{"source": "test-seed-data"}'
    );