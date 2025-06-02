-- Fixed test users creation script (includes auth_provider column)
-- Password hash for "password123": $2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.

-- Insert basic users with auth_provider column
INSERT INTO users (email, username, password, phone_number, auth_provider, enabled, role) VALUES
('ahmed.ben.ali@fsm.rnu.tn', 'ahmed_fsm', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '+21623456789', 'LOCAL', true, 'STUDENT'),
('fatma.gharbi@fsm.rnu.tn', 'fatma_info', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '+21624567890', 'LOCAL', true, 'STUDENT'),
('mohamed.sassi@fsm.rnu.tn', 'mohamed_cs', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '+21625678901', 'LOCAL', true, 'STUDENT'),
('sara.mahjoub@fsm.rnu.tn', 'sara_math', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '+21628901234', 'LOCAL', true, 'STUDENT'),
('karim.bouaziz@fsm.rnu.tn', 'karim_physics', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '+21629012345', 'LOCAL', true, 'STUDENT'),
('rania.khemiri@enis.rnu.tn', 'rania_soft', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '+21634567890', 'LOCAL', true, 'STUDENT'),
('bilel.guesmi@esprit.tn', 'bilel_code', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '+21635678901', 'LOCAL', true, 'STUDENT'),
('mariem.jlassi@fseg.rnu.tn', 'mariem_business', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '+21637890123', 'LOCAL', true, 'STUDENT');

-- Create user profiles for the created users
INSERT INTO user_profiles (full_name, field_of_study, institute, user_type, education_level, user_id) VALUES
('Ahmed Ben Ali', 'Science informatique', 'Faculté des sciences de Monastir', 'STUDENT', 'BACHELOR', (SELECT id FROM users WHERE username = 'ahmed_fsm')),
('Fatma Gharbi', 'Informatique', 'Faculté des sciences de Monastir', 'STUDENT', 'BACHELOR', (SELECT id FROM users WHERE username = 'fatma_info')),
('Mohamed Sassi', 'Génie informatique', 'Faculté des sciences de Monastir', 'STUDENT', 'BACHELOR', (SELECT id FROM users WHERE username = 'mohamed_cs')),
('Sara Mahjoub', 'Mathématiques', 'Faculté des sciences de Monastir', 'STUDENT', 'BACHELOR', (SELECT id FROM users WHERE username = 'sara_math')),
('Karim Bouaziz', 'Physique', 'Faculté des sciences de Monastir', 'STUDENT', 'BACHELOR', (SELECT id FROM users WHERE username = 'karim_physics')),
('Rania Khemiri', 'Génie logiciel', 'École Nationale d''Ingénieurs de Sfax', 'STUDENT', 'BACHELOR', (SELECT id FROM users WHERE username = 'rania_soft')),
('Bilel Guesmi', 'Informatique', 'ESPRIT', 'STUDENT', 'BACHELOR', (SELECT id FROM users WHERE username = 'bilel_code')),
('Mariem Jlassi', 'Business Administration', 'Faculté des Sciences Économiques et de Gestion', 'STUDENT', 'BACHELOR', (SELECT id FROM users WHERE username = 'mariem_business'));

-- Display created users
SELECT u.username, u.email, up.full_name, up.institute, up.field_of_study 
FROM users u 
LEFT JOIN user_profiles up ON u.id = up.user_id 
WHERE u.username IN ('ahmed_fsm', 'fatma_info', 'mohamed_cs', 'sara_math', 'karim_physics', 'rania_soft', 'bilel_code', 'mariem_business'); 