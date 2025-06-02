-- Add phone number columns to inquiries table
ALTER TABLE inquiries ADD COLUMN student_phone_number VARCHAR(20);
ALTER TABLE inquiries ADD COLUMN owner_phone_number VARCHAR(20);

-- Add index for phone number lookups (optional but good for performance)
CREATE INDEX idx_inquiries_student_phone ON inquiries(student_phone_number);
CREATE INDEX idx_inquiries_owner_phone ON inquiries(owner_phone_number); 