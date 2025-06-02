-- Create Test Users for Roommate Matching System
-- Compatible with user profile: Faculté des sciences de Monastir, Science informatique, Bachelor 3rd year

-- First, let's create users with varying compatibility levels
-- BCrypt hash for password "password123": $2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.

-- HIGH COMPATIBILITY USERS (Same institute + same/similar field)
INSERT INTO users (email, username, password, phone_number, provider, enabled, role, age) VALUES
('ahmed.ben.ali@fsm.rnu.tn', 'ahmed_fsm', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '+21623456789', 'LOCAL', true, 'STUDENT', 22),
('fatma.gharbi@fsm.rnu.tn', 'fatma_info', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '+21624567890', 'LOCAL', true, 'STUDENT', 21),
('mohamed.sassi@fsm.rnu.tn', 'mohamed_cs', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '+21625678901', 'LOCAL', true, 'STUDENT', 23),
('ines.ben.salem@fsm.rnu.tn', 'ines_dev', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '+21626789012', 'LOCAL', true, 'STUDENT', 22),
('youssef.triki@fsm.rnu.tn', 'youssef_tech', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '+21627890123', 'LOCAL', true, 'STUDENT', 24),

-- VERY GOOD COMPATIBILITY (Same institute + related fields)
('sara.mahjoub@fsm.rnu.tn', 'sara_math', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '+21628901234', 'LOCAL', true, 'STUDENT', 21),
('karim.bouaziz@fsm.rnu.tn', 'karim_physics', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '+21629012345', 'LOCAL', true, 'STUDENT', 23),
('leila.ben.rejeb@fsm.rnu.tn', 'leila_stat', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '+21630123456', 'LOCAL', true, 'STUDENT', 22),
('wassim.chakroun@fsm.rnu.tn', 'wassim_bio', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '+21631234567', 'LOCAL', true, 'STUDENT', 20),

-- GOOD COMPATIBILITY (Same institute + different fields but Masters level)
('nour.ben.ahmed@fsm.rnu.tn', 'nour_chemistry', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '+21632345678', 'LOCAL', true, 'STUDENT', 25),
('hamza.mnif@fsm.rnu.tn', 'hamza_geology', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '+21633456789', 'LOCAL', true, 'STUDENT', 24),

-- MEDIUM COMPATIBILITY (Different institute + similar field)
('rania.khemiri@enis.rnu.tn', 'rania_soft', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '+21634567890', 'LOCAL', true, 'STUDENT', 22),
('bilel.guesmi@esprit.tn', 'bilel_code', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '+21635678901', 'LOCAL', true, 'STUDENT', 23),
('asma.ben.salah@isg.rnu.tn', 'asma_sys', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '+21636789012', 'LOCAL', true, 'STUDENT', 21),

-- LOWER COMPATIBILITY (Different institute + different field)
('mariem.jlassi@fseg.rnu.tn', 'mariem_business', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '+21637890123', 'LOCAL', true, 'STUDENT', 22),
('sami.ben.mahmoud@flsh.rnu.tn', 'sami_literature', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '+21638901234', 'LOCAL', true, 'STUDENT', 24);

-- Now let's get the user IDs for profile creation
-- We'll create profiles for each user

-- HIGH COMPATIBILITY PROFILES (Same institute + same/similar field)
INSERT INTO user_profiles (full_name, date_of_birth, field_of_study, institute, user_type, student_year, education_level, user_id) VALUES
('Ahmed Ben Ali', '2002-03-15', 'Science informatique', 'Faculté des sciences de Monastir', 'STUDENT', '3ème année', 'BACHELOR', (SELECT id FROM users WHERE username = 'ahmed_fsm')),
('Fatma Gharbi', '2003-06-22', 'Informatique', 'Faculté des sciences de Monastir', 'STUDENT', '2ème année', 'BACHELOR', (SELECT id FROM users WHERE username = 'fatma_info')),
('Mohamed Sassi', '2001-09-10', 'Génie informatique', 'Faculté des sciences de Monastir', 'STUDENT', '3ème année', 'BACHELOR', (SELECT id FROM users WHERE username = 'mohamed_cs')),
('Ines Ben Salem', '2002-12-05', 'Science informatique', 'Faculté des sciences de Monastir', 'STUDENT', '3ème année', 'BACHELOR', (SELECT id FROM users WHERE username = 'ines_dev')),
('Youssef Triki', '2000-04-18', 'Informatique appliquée', 'Faculté des sciences de Monastir', 'STUDENT', '4ème année', 'BACHELOR', (SELECT id FROM users WHERE username = 'youssef_tech')),

-- VERY GOOD COMPATIBILITY PROFILES (Same institute + related fields)
('Sara Mahjoub', '2003-01-30', 'Mathématiques', 'Faculté des sciences de Monastir', 'STUDENT', '2ème année', 'BACHELOR', (SELECT id FROM users WHERE username = 'sara_math')),
('Karim Bouaziz', '2001-07-25', 'Physique', 'Faculté des sciences de Monastir', 'STUDENT', '3ème année', 'BACHELOR', (SELECT id FROM users WHERE username = 'karim_physics')),
('Leila Ben Rejeb', '2002-11-12', 'Statistiques et informatique', 'Faculté des sciences de Monastir', 'STUDENT', '3ème année', 'BACHELOR', (SELECT id FROM users WHERE username = 'leila_stat')),
('Wassim Chakroun', '2004-02-28', 'Biologie', 'Faculté des sciences de Monastir', 'STUDENT', '1ère année', 'BACHELOR', (SELECT id FROM users WHERE username = 'wassim_bio')),

-- GOOD COMPATIBILITY PROFILES (Same institute + different fields but Masters)
('Nour Ben Ahmed', '1999-05-14', 'Chimie', 'Faculté des sciences de Monastir', 'STUDENT', '1ère année', 'MASTERS', (SELECT id FROM users WHERE username = 'nour_chemistry')),
('Hamza Mnif', '2000-08-07', 'Géologie', 'Faculté des sciences de Monastir', 'STUDENT', '2ème année', 'MASTERS', (SELECT id FROM users WHERE username = 'hamza_geology')),

-- MEDIUM COMPATIBILITY PROFILES (Different institute + similar field)
('Rania Khemiri', '2002-10-20', 'Génie logiciel', 'École Nationale d''Ingénieurs de Sfax', 'STUDENT', '3ème année', 'BACHELOR', (SELECT id FROM users WHERE username = 'rania_soft')),
('Bilel Guesmi', '2001-12-03', 'Informatique', 'ESPRIT', 'STUDENT', '3ème année', 'BACHELOR', (SELECT id FROM users WHERE username = 'bilel_code')),
('Asma Ben Salah', '2003-03-16', 'Systèmes informatiques', 'Institut Supérieur de Gestion', 'STUDENT', '2ème année', 'BACHELOR', (SELECT id FROM users WHERE username = 'asma_sys')),

-- LOWER COMPATIBILITY PROFILES (Different institute + different field)
('Mariem Jlassi', '2002-07-09', 'Business Administration', 'Faculté des Sciences Économiques et de Gestion', 'STUDENT', '3ème année', 'BACHELOR', (SELECT id FROM users WHERE username = 'mariem_business')),
('Sami Ben Mahmoud', '2000-11-27', 'Littérature française', 'Faculté des Lettres et Sciences Humaines', 'STUDENT', '4ème année', 'BACHELOR', (SELECT id FROM users WHERE username = 'sami_literature'));

-- Create some user roommate preferences for better ML matching
INSERT INTO user_roommate_preferences (user_id, lifestyle_tags, cleanliness_level, social_level, study_habits, budget_min, budget_max, updated_at) VALUES
((SELECT id FROM users WHERE username = 'ahmed_fsm'), ARRAY['STUDIOUS', 'QUIET'], 4, 3, ARRAY['NIGHT_OWL', 'GROUP_STUDY'], 150.00, 300.00, NOW()),
((SELECT id FROM users WHERE username = 'fatma_info'), ARRAY['SOCIAL', 'STUDIOUS'], 5, 4, ARRAY['MORNING_PERSON', 'LIBRARY'], 120.00, 250.00, NOW()),
((SELECT id FROM users WHERE username = 'mohamed_cs'), ARRAY['TECH_ENTHUSIAST', 'QUIET'], 3, 2, ARRAY['NIGHT_OWL', 'CODING'], 180.00, 350.00, NOW()),
((SELECT id FROM users WHERE username = 'ines_dev'), ARRAY['SOCIAL', 'CREATIVE'], 4, 5, ARRAY['FLEXIBLE', 'GROUP_STUDY'], 140.00, 280.00, NOW()),
((SELECT id FROM users WHERE username = 'youssef_tech'), ARRAY['GAMER', 'STUDIOUS'], 3, 3, ARRAY['NIGHT_OWL', 'ONLINE'], 200.00, 400.00, NOW()),
((SELECT id FROM users WHERE username = 'sara_math'), ARRAY['STUDIOUS', 'QUIET'], 5, 2, ARRAY['MORNING_PERSON', 'SOLO_STUDY'], 130.00, 260.00, NOW()),
((SELECT id FROM users WHERE username = 'karim_physics'), ARRAY['INTELLECTUAL', 'SOCIAL'], 4, 4, ARRAY['FLEXIBLE', 'DISCUSSION'], 160.00, 320.00, NOW()),
((SELECT id FROM users WHERE username = 'leila_stat'), ARRAY['ANALYTICAL', 'ORGANIZED'], 5, 3, ARRAY['SCHEDULED', 'DATA'], 150.00, 300.00, NOW()),
((SELECT id FROM users WHERE username = 'rania_soft'), ARRAY['TECH_ENTHUSIAST', 'SOCIAL'], 4, 4, ARRAY['AGILE', 'PAIR_PROGRAMMING'], 170.00, 330.00, NOW()),
((SELECT id FROM users WHERE username = 'bilel_code'), ARRAY['GAMER', 'DEVELOPER'], 3, 3, ARRAY['NIGHT_OWL', 'HACKATHON'], 190.00, 380.00, NOW());

-- Display summary
SELECT 'Test users created successfully!' as result;
SELECT COUNT(*) as total_users_created FROM users WHERE email LIKE '%@fsm.rnu.tn' OR email LIKE '%@enis.rnu.tn' OR email LIKE '%@esprit.tn' OR email LIKE '%@isg.rnu.tn' OR email LIKE '%@fseg.rnu.tn' OR email LIKE '%@flsh.rnu.tn'; 