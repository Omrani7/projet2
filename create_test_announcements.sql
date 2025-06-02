-- Create Test Roommate Announcements (Fixed for actual table structure)
-- These announcements will have diverse compatibility levels for testing the ML algorithm

-- Insert roommate announcements using the actual column structure
INSERT INTO roommate_announcements (
    poster_id,
    property_title,
    property_address,
    property_latitude,
    property_longitude,
    total_rent,
    total_rooms,
    available_rooms,
    property_type,
    max_roommates,
    gender_preference,
    age_min,
    age_max,
    smoking_allowed,
    pets_allowed,
    cleanliness_level,
    rent_per_person,
    security_deposit,
    utilities_split,
    additional_costs,
    description,
    move_in_date,
    lease_duration_months,
    status,
    created_at,
    expires_at,
    updated_at
) VALUES

-- Announcement 1: Ahmed (same institute as user) - High compatibility expected
(3, 
 'Appartement 3 pièces près FSM',
 'Avenue de l''Environnement, Monastir',
 35.7781, 10.8264,
 1200.00, 3, 2,
 'APARTMENT',
 2, 'MIXED', 20, 25,
 false, false, 4,
 400.00, 400.00, 'EQUAL',
 'Internet et électricité inclus',
 'Appartement moderne à 5 min de la Faculté des Sciences de Monastir. Parfait pour étudiants sérieux. Chambre meublée avec bureau. Ambiance studieuse garantie!',
 '2025-02-01', 12, 'ACTIVE',
 NOW(), NOW() + INTERVAL '30 days', NOW()),

-- Announcement 2: Fatma (same institute + field) - Very high compatibility
(4,
 'Studio partageable centre Monastir',
 'Rue Habib Bourguiba, Monastir',
 35.7756, 10.8267,
 800.00, 2, 1,
 'STUDIO',
 1, 'FEMALE', 21, 24,
 false, false, 5,
 400.00, 300.00, 'EQUAL',
 'Charges comprises',
 'Studio cosy en centre-ville. Cherche colocataire féminine studieuse en informatique. Bibliothèque et cafés à proximité. Ambiance conviviale!',
 '2025-01-15', 10, 'ACTIVE',
 NOW(), NOW() + INTERVAL '25 days', NOW()),

-- Announcement 3: Sara (same institute, different field - math)
(6,
 'Maison étudiante Monastir',
 'Cité Universitaire, Monastir',
 35.7690, 10.8156,
 1500.00, 4, 3,
 'HOUSE',
 3, 'MIXED', 20, 23,
 false, true, 3,
 375.00, 500.00, 'USAGE_BASED',
 'Électricité selon consommation',
 'Grande maison près campus avec jardin. Idéal pour groupe d''étudiants. Ambiance détendue mais respectueuse des études. Animaux acceptés!',
 '2025-02-15', 12, 'ACTIVE',
 NOW(), NOW() + INTERVAL '40 days', NOW()),

-- Announcement 4: Salim (different institute - ENIS Sfax)
(12,
 'Appartement moderne Sfax',
 'Avenue Hédi Chaker, Sfax',
 34.7378, 10.7605,
 1000.00, 3, 2,
 'APARTMENT',
 2, 'MALE', 22, 26,
 false, false, 4,
 350.00, 350.00, 'EQUAL',
 'Charges incluses',
 'Appartement neuf près ENIS. Cherche colocataires ingénieurs. Bon pour études et détente. Terrasse avec vue sur mer!',
 '2025-01-20', 11, 'ACTIVE',
 NOW(), NOW() + INTERVAL '35 days', NOW()),

-- Announcement 5: Khalil (same institute, similar field)
(16,
 'Duplex étudiant Monastir',
 'Route de la Corniche, Monastir',
 35.7845, 10.8234,
 1400.00, 4, 2,
 'APARTMENT',
 2, 'MALE', 21, 24,
 false, false, 4,
 450.00, 450.00, 'EQUAL',
 'Internet haut débit inclus',
 'Duplex avec vue mer! Parfait pour étudiants en informatique. Espace de travail optimal, calme garanti. Proche FSM.',
 '2025-02-10', 12, 'ACTIVE',
 NOW(), NOW() + INTERVAL '28 days', NOW()),

-- Announcement 6: Hana (different institute - ESPRIT)
(21,
 'Colocation ESPRIT Tunis',
 'El Ghazala, Ariana',
 36.8983, 10.1894,
 1800.00, 3, 1,
 'APARTMENT',
 1, 'FEMALE', 22, 25,
 false, false, 5,
 600.00, 600.00, 'EQUAL',
 'Tout inclus',
 'Résidence haut standing près ESPRIT. Cherche colocataire ambitieuse en informatique. Environnement motivant!',
 '2025-01-25', 9, 'ACTIVE',
 NOW(), NOW() + INTERVAL '20 days', NOW()),

-- Announcement 7: Sonia (same institute, engineering field)
(17,
 'Appartement cosy Monastir',
 'Rue Ibn Khaldoun, Monastir',
 35.7767, 10.8298,
 900.00, 3, 2,
 'APARTMENT',
 2, 'FEMALE', 20, 23,
 false, false, 4,
 300.00, 200.00, 'EQUAL',
 'Eau et électricité à partager',
 'Appartement chaleureux à 2 min de la fac. Parfait pour étudiantes en génie informatique. Groupe soudé recherché!',
 '2025-02-05', 10, 'ACTIVE',
 NOW(), NOW() + INTERVAL '32 days', NOW()),

-- Announcement 8: Wael (business field - low compatibility expected)
(22,
 'Loft moderne business district',
 'Les Berges du Lac, Tunis',
 36.8425, 10.2359,
 2200.00, 2, 1,
 'APARTMENT',
 1, 'MALE', 23, 28,
 true, false, 3,
 900.00, 1000.00, 'EQUAL',
 'Services premium inclus',
 'Loft de luxe pour étudiant en business. Ambiance internationale, networking garanti. Parfait pour entrepreneur!',
 '2025-01-30', 12, 'ACTIVE',
 NOW(), NOW() + INTERVAL '45 days', NOW()),

-- Announcement 9: Mariem (same institute + field - very high compatibility)
(11,
 'Chambre dans villa Monastir',
 'Cité Riad, Monastir',
 35.7723, 10.8178,
 1100.00, 5, 3,
 'HOUSE',
 3, 'MIXED', 21, 24,
 false, true, 4,
 275.00, 300.00, 'EQUAL',
 'Internet et eau inclus',
 'Villa spacieuse avec jardin. Groupe d''étudiants en informatique FSM. Ambiance famille, entraide garantie!',
 '2025-02-20', 12, 'ACTIVE',
 NOW(), NOW() + INTERVAL '50 days', NOW()),

-- Announcement 10: Amine (ENIS - software engineering)
(20,
 'Résidence étudiante Sfax',
 'Campus universitaire, Sfax',
 34.7289, 10.7378,
 800.00, 2, 1,
 'STUDIO',
 1, 'MALE', 22, 25,
 false, false, 5,
 400.00, 300.00, 'EQUAL',
 'Services résidence inclus',
 'Résidence moderne campus ENIS. Cherche colocataire sérieux génie logiciel. Environnement studieux optimal!',
 '2025-01-18', 10, 'ACTIVE',
 NOW(), NOW() + INTERVAL '22 days', NOW());

-- Insert amenities for the announcements
INSERT INTO roommate_announcement_amenities (announcement_id, amenity) VALUES
-- Announcement 1 amenities
(1, 'WiFi'), (1, 'Climatisation'), (1, 'Parking'), (1, 'Cuisine équipée'),
-- Announcement 2 amenities
(2, 'WiFi'), (2, 'Climatisation'), (2, 'Près transport'),
-- Announcement 3 amenities
(3, 'WiFi'), (3, 'Jardin'), (3, 'Parking'), (3, 'Cuisine équipée'), (3, 'Salon commun'),
-- Announcement 4 amenities
(4, 'WiFi'), (4, 'Climatisation'), (4, 'Ascenseur'), (4, 'Terrasse'),
-- Announcement 5 amenities
(5, 'WiFi'), (5, 'Climatisation'), (5, 'Parking'), (5, 'Vue mer'), (5, 'Balcon'),
-- Announcement 6 amenities
(6, 'WiFi'), (6, 'Climatisation'), (6, 'Piscine'), (6, 'Sécurité'), (6, 'Transport'),
-- Announcement 7 amenities
(7, 'WiFi'), (7, 'Parking'), (7, 'Proche fac'),
-- Announcement 8 amenities
(8, 'WiFi'), (8, 'Gym'), (8, 'Piscine'), (8, 'Concierge'), (8, 'Business center'),
-- Announcement 9 amenities
(9, 'WiFi'), (9, 'Jardin'), (9, 'Parking'), (9, 'Terrasse'), (9, 'BBQ'),
-- Announcement 10 amenities
(10, 'WiFi'), (10, 'Bibliothèque'), (10, 'Salle sport'), (10, 'Cafétéria');

-- Insert lifestyle tags
INSERT INTO roommate_announcement_lifestyle_tags (announcement_id, lifestyle_tag) VALUES
-- Different lifestyle combinations for variety
(1, 'STUDIOUS'), (1, 'QUIET'),
(2, 'STUDIOUS'), (2, 'SOCIAL'),
(3, 'STUDIOUS'), (3, 'SOCIAL'),
(4, 'STUDIOUS'), (4, 'SOCIAL'),
(5, 'STUDIOUS'), (5, 'QUIET'),
(6, 'SOCIAL'), (6, 'STUDIOUS'),
(7, 'STUDIOUS'), (7, 'QUIET'),
(8, 'SOCIAL'), (8, 'PARTY'),
(9, 'STUDIOUS'), (9, 'SOCIAL'),
(10, 'STUDIOUS'), (10, 'QUIET');

-- Insert some image URLs (optional)
INSERT INTO roommate_announcement_images (announcement_id, image_url) VALUES
(1, 'apartment_monastir_1.jpg'), (1, 'apartment_monastir_2.jpg'),
(2, 'studio_monastir_1.jpg'),
(3, 'house_monastir_1.jpg'), (3, 'house_monastir_2.jpg'), (3, 'house_monastir_3.jpg'),
(4, 'apartment_sfax_1.jpg'), (4, 'apartment_sfax_2.jpg'),
(5, 'duplex_monastir_1.jpg'), (5, 'duplex_monastir_2.jpg'),
(6, 'esprit_apartment_1.jpg'), (6, 'esprit_apartment_2.jpg'),
(7, 'cosy_monastir_1.jpg'),
(8, 'loft_tunis_1.jpg'), (8, 'loft_tunis_2.jpg'),
(9, 'villa_monastir_1.jpg'), (9, 'villa_monastir_2.jpg'), (9, 'villa_monastir_3.jpg'),
(10, 'residence_sfax_1.jpg');

-- Add some sample applications for testing
INSERT INTO roommate_applications (announcement_id, applicant_id, poster_id, message, status, applied_at) VALUES
(1, 5, 3, 'Salut Ahmed! Je suis aussi en Science informatique à FSM. Ton annonce m''intéresse beaucoup!', 'PENDING', NOW() - INTERVAL '2 days'),
(1, 15, 3, 'Bonjour, étudiant en informatique FSM, cherche colocation sérieuse. Disponible février!', 'PENDING', NOW() - INTERVAL '1 day'),
(2, 8, 4, 'Hello Fatma! Étudiante en génie informatique FSM. Ton studio a l''air parfait!', 'PENDING', NOW() - INTERVAL '3 hours'),
(3, 18, 6, 'Bonjour Sara, étudiant en maths FSM. Votre maison semble idéale pour groupe d''étude!', 'PENDING', NOW() - INTERVAL '5 hours'),
(9, 3, 11, 'Salut Mariem! Ton annonce villa m''intéresse. Même formation, même fac!', 'PENDING', NOW() - INTERVAL '1 hour'); 