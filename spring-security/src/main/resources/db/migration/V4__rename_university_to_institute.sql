-- Rename university column to institute in user_profiles table
-- This migration supports the field name change from university to institute

ALTER TABLE user_profiles RENAME COLUMN university TO institute;

-- Add comment for documentation
COMMENT ON COLUMN user_profiles.institute IS 'Educational institute/university name where the student studies'; 