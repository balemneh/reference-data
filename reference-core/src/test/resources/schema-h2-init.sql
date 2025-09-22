-- H2 Initialization Script for Tests
-- This script configures H2 to behave more like PostgreSQL

-- Set case insensitive identifiers
SET DATABASE_TO_LOWER TRUE;
SET CASE_INSENSITIVE_IDENTIFIERS TRUE;

-- Create schema if needed (but we'll use default schema for tests)
-- CREATE SCHEMA IF NOT EXISTS reference_data;
-- SET SCHEMA reference_data;

-- Configure H2 to be more compatible with PostgreSQL
SET MODE PostgreSQL;

-- Ensure proper null ordering
SET DEFAULT_NULL_ORDERING HIGH;

-- Enable UUID type
CREATE DOMAIN IF NOT EXISTS UUID AS VARCHAR(36);

-- Create JSONB type alias for H2 (maps to TEXT)
CREATE DOMAIN IF NOT EXISTS JSONB AS TEXT;