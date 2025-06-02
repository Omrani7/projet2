-- Indexes for property_listings table
CREATE INDEX idx_property_listings_property_type ON property_listings (property_type);
CREATE INDEX idx_property_listings_price ON property_listings (price);
CREATE INDEX idx_property_listings_city ON property_listings (city);
CREATE INDEX idx_property_listings_district ON property_listings (district);
CREATE INDEX idx_property_listings_bedrooms ON property_listings (bedrooms);
CREATE INDEX idx_property_listings_rooms ON property_listings (rooms);
CREATE INDEX idx_property_listings_bathrooms ON property_listings (bathrooms);
CREATE INDEX idx_property_listings_area ON property_listings (area);
CREATE INDEX idx_property_listings_listing_date ON property_listings (listing_date);
CREATE INDEX idx_property_listings_active ON property_listings (active);
CREATE INDEX idx_property_listings_source_website ON property_listings (source_website);

-- Create spatial index if you have PostGIS extension installed
-- This is crucial for spatial queries in PropertyListingService
CREATE EXTENSION IF NOT EXISTS postgis;

-- Create a proper spatial column for efficient spatial operations
ALTER TABLE property_listings ADD COLUMN IF NOT EXISTS geom geometry(Point, 4326);

-- Update the geom column from latitude/longitude
UPDATE property_listings 
SET geom = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)
WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

-- Create a trigger to keep the geom column in sync with lat/long changes
CREATE OR REPLACE FUNCTION update_geom()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.latitude IS NOT NULL AND NEW.longitude IS NOT NULL THEN
    NEW.geom := ST_SetSRID(ST_MakePoint(NEW.longitude, NEW.latitude), 4326);
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_property_geom
BEFORE INSERT OR UPDATE ON property_listings
FOR EACH ROW EXECUTE FUNCTION update_geom();

-- Create spatial index
CREATE INDEX idx_property_listings_geom ON property_listings USING GIST (geom);

-- This index supports the image URL queries that were causing the N+1 problem
CREATE INDEX idx_property_listing_image_urls_property_id 
ON property_listing_image_urls (property_listing_id); 