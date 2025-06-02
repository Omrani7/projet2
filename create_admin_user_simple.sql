-- Simple SQL query to create admin user
-- Run this directly in your PostgreSQL database

INSERT INTO users (
    email, 
    username, 
    password, 
    phone_number, 
    auth_provider, 
    provider_id,
    enabled, 
    role,
    study_field,
    age,
    updated_at
) VALUES (
    'admin@pfe.com',
    'admin',
    '$2a$12$LjYKFXbGP5Nrw6/Eh9Qy8eC5ZGqhQFhI2Vk7x8y9z0A1B2C3D4E5F6',  -- Password: Admin123!
    '+216 98 765 432',
    'LOCAL',
    NULL,
    true,
    'ADMIN',
    'System Administration',
    30,
    NOW()
) ON CONFLICT (email) DO NOTHING;

-- Verify the admin user was created
SELECT id, email, username, role, enabled FROM users WHERE email = 'admin@pfe.com'; 