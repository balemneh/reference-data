-- Enable PostgreSQL extensions for full-text and fuzzy search
CREATE EXTENSION IF NOT EXISTS pg_trgm; -- For fuzzy/similarity search
CREATE EXTENSION IF NOT EXISTS unaccent; -- For accent-insensitive search
CREATE EXTENSION IF NOT EXISTS fuzzystrmatch; -- For soundex, levenshtein distance

-- Add full-text search columns and indexes for countries
ALTER TABLE reference_data.countries_v 
ADD COLUMN IF NOT EXISTS search_vector tsvector;

-- Create function to generate search vector
CREATE OR REPLACE FUNCTION reference_data.countries_search_vector_trigger() 
RETURNS trigger AS $$
BEGIN
    NEW.search_vector := 
        setweight(to_tsvector('english', coalesce(NEW.country_name, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(NEW.country_code, '')), 'B') ||
        setweight(to_tsvector('english', coalesce(NEW.iso2_code, '')), 'C') ||
        setweight(to_tsvector('english', coalesce(NEW.iso3_code, '')), 'C');
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

-- Create trigger to update search vector
DROP TRIGGER IF EXISTS countries_search_vector_update ON reference_data.countries_v;
CREATE TRIGGER countries_search_vector_update 
BEFORE INSERT OR UPDATE ON reference_data.countries_v
FOR EACH ROW EXECUTE FUNCTION reference_data.countries_search_vector_trigger();

-- Create GIN index for full-text search
CREATE INDEX IF NOT EXISTS idx_countries_search_vector 
ON reference_data.countries_v USING GIN(search_vector);

-- Create trigram indexes for fuzzy search
CREATE INDEX IF NOT EXISTS idx_countries_name_trgm 
ON reference_data.countries_v USING GIN(country_name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_countries_code_trgm 
ON reference_data.countries_v USING GIN(country_code gin_trgm_ops);

-- Update existing records
UPDATE reference_data.countries_v 
SET search_vector = 
    setweight(to_tsvector('english', coalesce(country_name, '')), 'A') ||
    setweight(to_tsvector('english', coalesce(country_code, '')), 'B') ||
    setweight(to_tsvector('english', coalesce(iso2_code, '')), 'C') ||
    setweight(to_tsvector('english', coalesce(iso3_code, '')), 'C');

-- Create stored functions for unified search

-- Fuzzy search function for countries
CREATE OR REPLACE FUNCTION reference_data.search_countries(
    search_term TEXT,
    similarity_threshold FLOAT DEFAULT 0.3,
    limit_results INT DEFAULT 100
)
RETURNS TABLE (
    id UUID,
    country_code VARCHAR,
    country_name VARCHAR,
    iso2_code VARCHAR,
    iso3_code VARCHAR,
    similarity_score FLOAT,
    rank FLOAT
) AS $$
BEGIN
    RETURN QUERY
    SELECT DISTINCT ON (c.country_code)
        c.id,
        c.country_code,
        c.country_name,
        c.iso2_code,
        c.iso3_code,
        GREATEST(
            similarity(c.country_name, search_term),
            similarity(c.country_code, search_term),
            similarity(c.iso2_code, search_term),
            similarity(c.iso3_code, search_term)
        ) as similarity_score,
        ts_rank(c.search_vector, plainto_tsquery('english', search_term)) as rank
    FROM reference_data.countries_v c
    WHERE 
        -- Full-text search
        c.search_vector @@ plainto_tsquery('english', search_term)
        OR
        -- Fuzzy search with trigram similarity
        (
            similarity(c.country_name, search_term) > similarity_threshold OR
            similarity(c.country_code, search_term) > similarity_threshold OR
            similarity(c.iso2_code, search_term) > similarity_threshold OR
            similarity(c.iso3_code, search_term) > similarity_threshold
        )
        AND (c.valid_to IS NULL OR c.valid_to > CURRENT_DATE)
    ORDER BY 
        c.country_code,
        similarity_score DESC,
        rank DESC
    LIMIT limit_results;
END;
$$ LANGUAGE plpgsql;

-- Universal search across reference data types (currently only countries)
CREATE OR REPLACE FUNCTION reference_data.universal_search(
    search_term TEXT,
    data_types TEXT[] DEFAULT ARRAY['countries'],
    similarity_threshold FLOAT DEFAULT 0.3,
    limit_per_type INT DEFAULT 20
)
RETURNS TABLE (
    entity_type VARCHAR,
    entity_id UUID,
    code VARCHAR,
    name VARCHAR,
    additional_info JSONB,
    similarity_score FLOAT,
    rank FLOAT
) AS $$
BEGIN
    RETURN QUERY
    -- Search countries
    SELECT 
        'country'::VARCHAR as entity_type,
        c.id as entity_id,
        c.country_code as code,
        c.country_name as name,
        jsonb_build_object(
            'iso2_code', c.iso2_code,
            'iso3_code', c.iso3_code,
            'numeric_code', c.numeric_code
        ) as additional_info,
        c.similarity_score,
        c.rank
    FROM reference_data.search_countries(search_term, similarity_threshold, limit_per_type) c
    WHERE 'countries' = ANY(data_types)
    
    ORDER BY similarity_score DESC, rank DESC;
END;
$$ LANGUAGE plpgsql;

-- Create a function for autocomplete/typeahead
CREATE OR REPLACE FUNCTION reference_data.autocomplete(
    prefix TEXT,
    data_type VARCHAR DEFAULT 'countries',
    limit_results INT DEFAULT 10
)
RETURNS TABLE (
    code VARCHAR,
    name VARCHAR,
    match_type VARCHAR
) AS $$
BEGIN
    prefix := lower(prefix);
    
    IF data_type = 'countries' THEN
        RETURN QUERY
        SELECT 
            c.country_code as code,
            c.country_name as name,
            CASE 
                WHEN lower(c.country_code) LIKE prefix || '%' THEN 'code'
                WHEN lower(c.country_name) LIKE prefix || '%' THEN 'name'
                ELSE 'fuzzy'
            END as match_type
        FROM reference_data.countries_v c
        WHERE 
            (c.valid_to IS NULL OR c.valid_to > CURRENT_DATE)
            AND (
                lower(c.country_code) LIKE prefix || '%' OR
                lower(c.country_name) LIKE prefix || '%' OR
                lower(c.iso2_code) LIKE prefix || '%' OR
                lower(c.iso3_code) LIKE prefix || '%'
            )
        ORDER BY 
            CASE 
                WHEN lower(c.country_code) LIKE prefix || '%' THEN 1
                WHEN lower(c.iso2_code) LIKE prefix || '%' THEN 2
                WHEN lower(c.iso3_code) LIKE prefix || '%' THEN 3
                WHEN lower(c.country_name) LIKE prefix || '%' THEN 4
                ELSE 5
            END,
            c.country_name
        LIMIT limit_results;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Add comments for documentation
COMMENT ON FUNCTION reference_data.search_countries IS 'Full-text and fuzzy search for countries with similarity scoring';
COMMENT ON FUNCTION reference_data.universal_search IS 'Search across reference data types with unified results';
COMMENT ON FUNCTION reference_data.autocomplete IS 'Fast prefix-based autocomplete for reference data';