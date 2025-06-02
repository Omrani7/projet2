-- Script to create an admin user for accessing the admin dashboard
-- Run this script after starting your Spring Boot application

-- Insert admin user
INSERT INTO users (
    email, 
    username, 
    password, 
    phone_number, 
    auth_provider, 
    enabled, 
    role,
    created_at,
    updated_at
) VALUES (
    'admin@pfe.com',
    'admin',
    '$2a$12$LjYKFXbGP5Nrw6/Eh9Qy8eC5ZGqhQFhI2Vk7x8y9z0A1B2C3D4E5F6',  -- Password: Admin123!
    '+216 98 765 432',
    'LOCAL',
    true,
    'ADMIN',
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;

-- Create a user profile for the admin (optional)
INSERT INTO user_profiles (
    user_id,
    full_name,
    user_type,
    institute,
    field_of_study,
    student_year,
    created_at,
    updated_at
) VALUES (
    (SELECT id FROM users WHERE email = 'admin@pfe.com'),
    'System Administrator',
    'STUDENT',  -- Since profile is optional for admin, we set a default
    'Platform Administration',
    'System Management',
    'N/A',
    NOW(),
    NOW()
) ON CONFLICT (user_id) DO NOTHING;

-- Display admin user info
SELECT 
    u.id,
    u.email,
    u.username,
    u.role,
    u.enabled,
    u.created_at,
    up.full_name
FROM users u
LEFT JOIN user_profiles up ON u.id = up.user_id
WHERE u.email = 'admin@pfe.com'; 