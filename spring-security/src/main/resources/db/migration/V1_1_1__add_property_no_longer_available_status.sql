-- Migration to add PROPERTY_NO_LONGER_AVAILABLE status to inquiries table
-- This updates the check constraint to include the new status value

-- Drop the existing constraint
ALTER TABLE inquiries DROP CONSTRAINT IF EXISTS inquiries_status_check;

-- Add the updated constraint with the new status
ALTER TABLE inquiries ADD CONSTRAINT inquiries_status_check 
    CHECK (status IN ('PENDING_REPLY', 'REPLIED', 'CLOSED', 'PROPERTY_NO_LONGER_AVAILABLE'));

-- Optional: Add comment for documentation
COMMENT ON CONSTRAINT inquiries_status_check ON inquiries 
IS 'Ensures inquiry status is one of the valid enum values including PROPERTY_NO_LONGER_AVAILABLE'; 