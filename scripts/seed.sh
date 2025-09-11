#!/bin/bash

# CBP Reference Data Service - Seed Data Script
# This script loads initial reference data into the system

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-reference_db}"
DB_USER="${DB_USER:-reference_user}"
DB_SCHEMA="${DB_SCHEMA:-reference_data}"
API_URL="${API_URL:-http://localhost:8080}"

echo -e "${BLUE}🌱 CBP Reference Data Service - Seed Script${NC}"
echo -e "${BLUE}=============================================${NC}"

# Function to check if PostgreSQL is ready
check_postgres() {
    echo -e "${YELLOW}⏳ Checking PostgreSQL connection...${NC}"
    until pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" > /dev/null 2>&1; do
        echo -e "${YELLOW}   Waiting for PostgreSQL...${NC}"
        sleep 2
    done
    echo -e "${GREEN}✅ PostgreSQL is ready${NC}"
}

# Function to check if API is ready
check_api() {
    echo -e "${YELLOW}⏳ Checking API readiness...${NC}"
    local retries=30
    while [ $retries -gt 0 ]; do
        if curl -f "$API_URL/actuator/health" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ API is ready${NC}"
            return 0
        fi
        echo -e "${YELLOW}   Waiting for API... ($retries attempts remaining)${NC}"
        sleep 5
        ((retries--))
    done
    echo -e "${RED}❌ API not ready after timeout${NC}"
    return 1
}

# Function to seed code systems
seed_code_systems() {
    echo -e "${BLUE}📊 Seeding code systems...${NC}"
    
    # Insert additional code systems beyond the ones in migrations
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" << 'EOF'
INSERT INTO reference_data.code_system (id, code, name, description, owner, created_at, updated_at)
VALUES 
    (gen_random_uuid(), 'GENC', 'GENC Country Codes', 'Geopolitical Entities and Codes', 'NGA', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'IATA', 'IATA Airport Codes', 'International Air Transport Association codes', 'IATA', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'ICAO', 'ICAO Airport Codes', 'International Civil Aviation Organization codes', 'ICAO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'CBP-PORTS', 'CBP Port Codes', 'CBP port identification codes', 'CBP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'UNLOCODE', 'UN/LOCODE', 'United Nations Code for Trade and Transport Locations', 'UNECE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'SCAC', 'SCAC Codes', 'Standard Carrier Alpha Codes', 'NMFTA', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP;
EOF
    
    echo -e "${GREEN}✅ Code systems seeded${NC}"
}

# Function to seed sample countries
seed_countries() {
    echo -e "${BLUE}🌍 Seeding sample countries...${NC}"
    
    # Get ISO3166-1 code system ID
    local iso_system_id
    iso_system_id=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT id FROM reference_data.code_system WHERE code = 'ISO3166-1';")
    iso_system_id=$(echo "$iso_system_id" | xargs) # trim whitespace
    
    # Insert sample countries
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" << EOF
INSERT INTO reference_data.countries_v (
    id, version, code_system_id, country_code, country_name, 
    iso2_code, iso3_code, numeric_code, is_active,
    valid_from, recorded_at, recorded_by
) VALUES 
    (gen_random_uuid(), 1, '$iso_system_id', 'US', 'United States of America', 'US', 'USA', '840', true, CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT'),
    (gen_random_uuid(), 1, '$iso_system_id', 'CA', 'Canada', 'CA', 'CAN', '124', true, CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT'),
    (gen_random_uuid(), 1, '$iso_system_id', 'MX', 'Mexico', 'MX', 'MEX', '484', true, CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT'),
    (gen_random_uuid(), 1, '$iso_system_id', 'GB', 'United Kingdom', 'GB', 'GBR', '826', true, CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT'),
    (gen_random_uuid(), 1, '$iso_system_id', 'FR', 'France', 'FR', 'FRA', '250', true, CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT'),
    (gen_random_uuid(), 1, '$iso_system_id', 'DE', 'Germany', 'DE', 'DEU', '276', true, CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT'),
    (gen_random_uuid(), 1, '$iso_system_id', 'JP', 'Japan', 'JP', 'JPN', '392', true, CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT'),
    (gen_random_uuid(), 1, '$iso_system_id', 'CN', 'China', 'CN', 'CHN', '156', true, CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT'),
    (gen_random_uuid(), 1, '$iso_system_id', 'AU', 'Australia', 'AU', 'AUS', '036', true, CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT'),
    (gen_random_uuid(), 1, '$iso_system_id', 'BR', 'Brazil', 'BR', 'BRA', '076', true, CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT')
ON CONFLICT DO NOTHING;
EOF
    
    echo -e "${GREEN}✅ Sample countries seeded${NC}"
}

# Function to seed sample ports
seed_ports() {
    echo -e "${BLUE}🚢 Seeding sample ports...${NC}"
    
    # Get CBP-PORTS code system ID
    local cbp_system_id
    cbp_system_id=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT id FROM reference_data.code_system WHERE code = 'CBP-PORTS';")
    cbp_system_id=$(echo "$cbp_system_id" | xargs)
    
    # Insert sample ports
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" << EOF
INSERT INTO reference_data.ports_v (
    id, version, code_system_id, port_code, port_name, city, state_province, country_code,
    latitude, longitude, port_type, is_active, un_locode, cbp_port_code, timezone,
    valid_from, recorded_at, recorded_by
) VALUES 
    (gen_random_uuid(), 1, '$cbp_system_id', '2704', 'Port of Los Angeles', 'Los Angeles', 'CA', 'USA', 33.7167, -118.2667, 'SEAPORT', true, 'USLAX', '2704', 'America/Los_Angeles', CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT'),
    (gen_random_uuid(), 1, '$cbp_system_id', '2708', 'Port of Long Beach', 'Long Beach', 'CA', 'USA', 33.7553, -118.2222, 'SEAPORT', true, 'USLGB', '2708', 'America/Los_Angeles', CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT'),
    (gen_random_uuid(), 1, '$cbp_system_id', '4601', 'Port of New York/New Jersey', 'New York', 'NY', 'USA', 40.6667, -74.0333, 'SEAPORT', true, 'USNYC', '4601', 'America/New_York', CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT'),
    (gen_random_uuid(), 1, '$cbp_system_id', '5301', 'Port of Miami', 'Miami', 'FL', 'USA', 25.7742, -80.1936, 'SEAPORT', true, 'USMIA', '5301', 'America/New_York', CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT'),
    (gen_random_uuid(), 1, '$cbp_system_id', '2602', 'Port of Seattle', 'Seattle', 'WA', 'USA', 47.5950, -122.3344, 'SEAPORT', true, 'USSEA', '2602', 'America/Los_Angeles', CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT')
ON CONFLICT DO NOTHING;
EOF
    
    echo -e "${GREEN}✅ Sample ports seeded${NC}"
}

# Function to seed sample airports
seed_airports() {
    echo -e "${BLUE}✈️  Seeding sample airports...${NC}"
    
    # Insert sample airports
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" << 'EOF'
INSERT INTO reference_data.airports_v (
    id, version, iata_code, icao_code, airport_name, city, state_province, country_code,
    latitude, longitude, elevation_ft, airport_type, is_international, is_active, timezone,
    has_customs, has_immigration, valid_from, recorded_at, recorded_by
) VALUES 
    (gen_random_uuid(), 1, 'LAX', 'KLAX', 'Los Angeles International Airport', 'Los Angeles', 'CA', 'USA', 33.9425, -118.4081, 125, 'LARGE_AIRPORT', true, true, 'America/Los_Angeles', true, true, CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT'),
    (gen_random_uuid(), 1, 'JFK', 'KJFK', 'John F. Kennedy International Airport', 'New York', 'NY', 'USA', 40.6413, -73.7781, 13, 'LARGE_AIRPORT', true, true, 'America/New_York', true, true, CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT'),
    (gen_random_uuid(), 1, 'MIA', 'KMIA', 'Miami International Airport', 'Miami', 'FL', 'USA', 25.7933, -80.2906, 8, 'LARGE_AIRPORT', true, true, 'America/New_York', true, true, CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT'),
    (gen_random_uuid(), 1, 'ORD', 'KORD', 'Chicago O''Hare International Airport', 'Chicago', 'IL', 'USA', 41.9742, -87.9073, 672, 'LARGE_AIRPORT', true, true, 'America/Chicago', true, true, CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT'),
    (gen_random_uuid(), 1, 'YYZ', 'CYYZ', 'Toronto Pearson International Airport', 'Toronto', 'ON', 'CAN', 43.6777, -79.6248, 569, 'LARGE_AIRPORT', true, true, 'America/Toronto', true, true, CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT')
ON CONFLICT DO NOTHING;
EOF
    
    echo -e "${GREEN}✅ Sample airports seeded${NC}"
}

# Function to seed sample carriers
seed_carriers() {
    echo -e "${BLUE}🚛 Seeding sample carriers...${NC}"
    
    # Get IATA code system ID
    local iata_system_id
    iata_system_id=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT id FROM reference_data.code_system WHERE code = 'IATA';")
    iata_system_id=$(echo "$iata_system_id" | xargs)
    
    # Insert sample carriers
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" << EOF
INSERT INTO reference_data.carriers_v (
    id, version, code_system_id, carrier_code, carrier_name, iata_code, icao_code, country_code,
    carrier_type, is_active, operating_name, is_passenger_carrier, is_cargo_carrier,
    valid_from, recorded_at, recorded_by
) VALUES 
    (gen_random_uuid(), 1, '$iata_system_id', 'AA', 'American Airlines', 'AA', 'AAL', 'USA', 'AIRLINE', true, 'American Airlines', true, false, CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT'),
    (gen_random_uuid(), 1, '$iata_system_id', 'DL', 'Delta Air Lines', 'DL', 'DAL', 'USA', 'AIRLINE', true, 'Delta', true, false, CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT'),
    (gen_random_uuid(), 1, '$iata_system_id', 'UA', 'United Airlines', 'UA', 'UAL', 'USA', 'AIRLINE', true, 'United', true, false, CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT'),
    (gen_random_uuid(), 1, '$iata_system_id', 'AC', 'Air Canada', 'AC', 'ACA', 'CAN', 'AIRLINE', true, 'Air Canada', true, false, CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT'),
    (gen_random_uuid(), 1, '$iata_system_id', 'FX', 'FedEx', 'FX', 'FDX', 'USA', 'CARGO', true, 'FedEx Express', false, true, CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT')
ON CONFLICT DO NOTHING;
EOF
    
    echo -e "${GREEN}✅ Sample carriers seeded${NC}"
}

# Function to create sample code mappings
seed_mappings() {
    echo -e "${BLUE}🔗 Seeding sample code mappings...${NC}"
    
    # Get system IDs
    local iso_id cbp_id
    iso_id=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT id FROM reference_data.code_system WHERE code = 'ISO3166-1';")
    cbp_id=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT id FROM reference_data.code_system WHERE code = 'CBP-COUNTRY5';")
    iso_id=$(echo "$iso_id" | xargs)
    cbp_id=$(echo "$cbp_id" | xargs)
    
    # Insert sample mappings
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" << EOF
INSERT INTO reference_data.code_mapping (
    id, version, from_system_id, from_code, to_system_id, to_code,
    rule_id, confidence, mapping_type, is_deprecated,
    valid_from, recorded_at, recorded_by
) VALUES 
    (gen_random_uuid(), 1, '$iso_id', 'US', '$cbp_id', 'USOFA', 'DIRECT_MAP', 100.00, 'DIRECT', false, CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT'),
    (gen_random_uuid(), 1, '$iso_id', 'CA', '$cbp_id', 'CANDA', 'DIRECT_MAP', 100.00, 'DIRECT', false, CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT'),
    (gen_random_uuid(), 1, '$iso_id', 'MX', '$cbp_id', 'MEXIC', 'DIRECT_MAP', 100.00, 'DIRECT', false, CURRENT_DATE, CURRENT_TIMESTAMP, 'SEED_SCRIPT')
ON CONFLICT DO NOTHING;
EOF
    
    echo -e "${GREEN}✅ Sample code mappings seeded${NC}"
}

# Main execution
main() {
    echo -e "${BLUE}Starting seed process...${NC}"
    
    # Check if postgres is available
    check_postgres
    
    # Seed database
    seed_code_systems
    seed_countries
    seed_ports
    seed_airports
    seed_carriers
    seed_mappings
    
    # Wait for API to be ready before final checks
    if check_api; then
        echo -e "${BLUE}🔍 Running validation checks...${NC}"
        
        # Simple API validation
        local country_count
        country_count=$(curl -s "$API_URL/v1/countries/current" | jq length 2>/dev/null || echo "0")
        echo -e "${GREEN}   Countries loaded: $country_count${NC}"
        
        # Check dataset stats
        if curl -f "$API_URL/v1/datasets/stats" > /dev/null 2>&1; then
            echo -e "${GREEN}   Dataset stats endpoint working${NC}"
        fi
    fi
    
    echo -e "${GREEN}🎉 Seed process completed successfully!${NC}"
    echo -e "${BLUE}=============================================${NC}"
    echo -e "${YELLOW}You can now access:${NC}"
    echo -e "${YELLOW}  - API: $API_URL${NC}"
    echo -e "${YELLOW}  - Swagger UI: $API_URL/swagger-ui${NC}"
    echo -e "${YELLOW}  - Health Check: $API_URL/actuator/health${NC}"
}

# Check if required tools are available
command -v psql >/dev/null 2>&1 || { echo -e "${RED}❌ psql is required but not installed${NC}"; exit 1; }
command -v curl >/dev/null 2>&1 || { echo -e "${RED}❌ curl is required but not installed${NC}"; exit 1; }

# Run main function
main "$@"