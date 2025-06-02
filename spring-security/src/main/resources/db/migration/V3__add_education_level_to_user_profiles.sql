-- Add education_level column to user_profiles table
ALTER TABLE user_profiles ADD COLUMN education_level VARCHAR(20);

-- Add a check constraint to ensure only valid values are allowed
ALTER TABLE user_profiles ADD CONSTRAINT chk_education_level 
    CHECK (education_level IN ('BACHELOR', 'MASTERS', 'PHD')); 