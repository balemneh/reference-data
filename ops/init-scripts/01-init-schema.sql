-- Create schemas
CREATE SCHEMA IF NOT EXISTS reference_data;
CREATE SCHEMA IF NOT EXISTS staging;
CREATE SCHEMA IF NOT EXISTS workflow;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Set search path
SET search_path TO reference_data, public;

-- Grant permissions
GRANT ALL ON SCHEMA reference_data TO refdata_user;
GRANT ALL ON SCHEMA staging TO refdata_user;
GRANT ALL ON SCHEMA workflow TO refdata_user;