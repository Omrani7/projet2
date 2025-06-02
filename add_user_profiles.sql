-- Add user profiles for test users to improve compatibility matching
-- This will provide the missing institute, fieldOfStudy, and educationLevel data

-- Update existing user (your profile) with proper education level
UPDATE user_profiles 
SET education_level = 'BACHELOR'
WHERE user_id = 2;

-- Insert profiles for users that don't have them yet
INSERT INTO user_profiles (user_id, full_name, field_of_study, institute, education_level, user_type) VALUES
(3, 'Ahmed Ben Ali', 'Science informatique', 'Faculté des sciences de Monastir', 'BACHELOR', 'STUDENT'),
(4, 'Fatma Gharbi', 'Science informatique', 'Faculté des sciences de Monastir', 'BACHELOR', 'STUDENT'),
(5, 'Mohamed Sassi', 'Science informatique', 'Faculté des sciences de Monastir', 'BACHELOR', 'STUDENT'),
(6, 'Sara Mahjoub', 'Mathématiques', 'Faculté des sciences de Monastir', 'BACHELOR', 'STUDENT'),
(7, 'Youssef Trabelsi', 'Physique', 'Faculté des sciences de Monastir', 'BACHELOR', 'STUDENT'),
(8, 'Amira Khelifi', 'Génie informatique', 'Faculté des sciences de Monastir', 'BACHELOR', 'STUDENT'),
(11, 'Mariem Nasri', 'Science informatique', 'Faculté des sciences de Monastir', 'BACHELOR', 'STUDENT'),
(12, 'Salim Bouaziz', 'Informatique', 'École Nationale d''Ingénieurs de Sfax', 'BACHELOR', 'STUDENT'),
(13, 'Nour Khemiri', 'Génie logiciel', 'École Nationale d''Ingénieurs de Sfax', 'BACHELOR', 'STUDENT'),
(15, 'Rania Mliki', 'Science informatique', 'Faculté des sciences de Monastir', 'BACHELOR', 'STUDENT'),
(16, 'Khalil Jendoubi', 'Informatique', 'Faculté des sciences de Monastir', 'BACHELOR', 'STUDENT'),
(17, 'Sonia Fredj', 'Génie informatique', 'Faculté des sciences de Monastir', 'BACHELOR', 'STUDENT'),
(18, 'Farouk Mansouri', 'Mathématiques', 'Faculté des sciences de Monastir', 'BACHELOR', 'STUDENT'),
(19, 'Meriem Sellami', 'Physique', 'Faculté des sciences de Monastir', 'BACHELOR', 'STUDENT'),
(20, 'Amine Dridi', 'Génie logiciel', 'École Nationale d''Ingénieurs de Sfax', 'BACHELOR', 'STUDENT'),
(21, 'Hana Guesmi', 'Informatique', 'ESPRIT', 'BACHELOR', 'STUDENT'),
(22, 'Wael Chebbi', 'Business Administration', 'Faculté des Sciences Économiques et de Gestion', 'BACHELOR', 'STUDENT')
ON CONFLICT (user_id) DO UPDATE SET
  field_of_study = EXCLUDED.field_of_study,
  institute = EXCLUDED.institute,
  education_level = EXCLUDED.education_level,
  user_type = EXCLUDED.user_type;

-- Update ages for users to improve age compatibility matching
UPDATE users SET age = 22 WHERE id IN (3, 4, 5, 11, 15, 16, 17);
UPDATE users SET age = 21 WHERE id IN (6, 7, 8, 18, 19);
UPDATE users SET age = 23 WHERE id IN (12, 13, 20, 21, 22);

-- Update study fields in users table to match profiles
UPDATE users SET study_field = 'Science informatique' WHERE id IN (2, 3, 4, 5, 11, 15);
UPDATE users SET study_field = 'Informatique' WHERE id IN (16, 21);
UPDATE users SET study_field = 'Génie informatique' WHERE id IN (8, 17);
UPDATE users SET study_field = 'Mathématiques' WHERE id IN (6, 18);
UPDATE users SET study_field = 'Physique' WHERE id IN (7, 19);
UPDATE users SET study_field = 'Génie logiciel' WHERE id IN (12, 13, 20);
UPDATE users SET study_field = 'Business Administration' WHERE id = 22; 