-- Create roommate announcement tables following existing patterns
-- Using BIGINT (Long) IDs to match existing entity structure

-- Roommate Announcements
CREATE TABLE roommate_announcements (
    id BIGSERIAL PRIMARY KEY,
    poster_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    property_listing_id BIGINT REFERENCES property_listings(id) ON DELETE SET NULL, -- For Type A announcements
    
    -- Property details (stored as JSON for flexibility)
    property_title VARCHAR(500) NOT NULL,
    property_address TEXT NOT NULL,
    property_latitude DOUBLE PRECISION,
    property_longitude DOUBLE PRECISION,
    total_rent DECIMAL(10,2) NOT NULL,
    total_rooms INTEGER NOT NULL,
    available_rooms INTEGER NOT NULL,
    property_type VARCHAR(50) NOT NULL, -- 'APARTMENT', 'HOUSE', 'STUDIO'
    amenities TEXT[], -- Array of amenity strings
    image_urls TEXT[], -- Array of image URL strings
    
    -- Roommate preferences (JSON for complex structures)
    max_roommates INTEGER NOT NULL,
    gender_preference VARCHAR(20) NOT NULL DEFAULT 'NO_PREFERENCE', -- 'MALE', 'FEMALE', 'MIXED', 'NO_PREFERENCE'
    age_min INTEGER NOT NULL DEFAULT 18,
    age_max INTEGER NOT NULL DEFAULT 35,
    lifestyle_tags TEXT[], -- Array like ['QUIET', 'SOCIAL', 'STUDIOUS']
    smoking_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    pets_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    cleanliness_level INTEGER NOT NULL DEFAULT 3 CHECK (cleanliness_level BETWEEN 1 AND 5),
    
    -- Financial details
    rent_per_person DECIMAL(10,2) NOT NULL,
    security_deposit DECIMAL(10,2) NOT NULL DEFAULT 0,
    utilities_split VARCHAR(20) NOT NULL DEFAULT 'EQUAL', -- 'EQUAL', 'USAGE_BASED'
    additional_costs TEXT,
    
    -- Posting details
    description TEXT,
    move_in_date DATE NOT NULL,
    lease_duration_months INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'PAUSED', 'FILLED', 'EXPIRED'
    
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Roommate Applications
CREATE TABLE roommate_applications (
    id BIGSERIAL PRIMARY KEY,
    announcement_id BIGINT NOT NULL REFERENCES roommate_announcements(id) ON DELETE CASCADE,
    applicant_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    poster_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    message TEXT NOT NULL,
    compatibility_score DECIMAL(3,2), -- Calculated by ML algorithm
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'ACCEPTED', 'REJECTED', 'WITHDRAWN'
    
    applied_at TIMESTAMP NOT NULL DEFAULT NOW(),
    responded_at TIMESTAMP,
    
    UNIQUE(announcement_id, applicant_id) -- Prevent duplicate applications
);

-- Conversations for messaging
CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,
    announcement_id BIGINT REFERENCES roommate_announcements(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Conversation participants (many-to-many)
CREATE TABLE conversation_participants (
    conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (conversation_id, user_id)
);

-- Messages
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    message_type VARCHAR(30) NOT NULL DEFAULT 'TEXT', -- 'TEXT', 'IMAGE', 'ANNOUNCEMENT_REFERENCE'
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    is_read BOOLEAN NOT NULL DEFAULT FALSE
);

-- User roommate preferences (extends existing user profiles)
CREATE TABLE user_roommate_preferences (
    user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    lifestyle_tags TEXT[],
    cleanliness_level INTEGER CHECK (cleanliness_level BETWEEN 1 AND 5),
    social_level INTEGER CHECK (social_level BETWEEN 1 AND 5),
    study_habits TEXT[],
    budget_min DECIMAL(10,2),
    budget_max DECIMAL(10,2),
    preferred_location_latitude DOUBLE PRECISION,
    preferred_location_longitude DOUBLE PRECISION,
    location_radius_km INTEGER DEFAULT 10,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Roommate matches (for tracking ML recommendations)
CREATE TABLE roommate_matches (
    id BIGSERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recommended_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    announcement_id BIGINT REFERENCES roommate_announcements(id) ON DELETE CASCADE,
    compatibility_score DECIMAL(3,2) NOT NULL,
    match_factors TEXT, -- JSON string of factors that contributed to match
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE(user_id, recommended_user_id, announcement_id)
);

-- Create indexes for performance (following existing patterns)
CREATE INDEX idx_roommate_announcements_poster ON roommate_announcements(poster_id);
CREATE INDEX idx_roommate_announcements_status ON roommate_announcements(status);
CREATE INDEX idx_roommate_announcements_location ON roommate_announcements(property_latitude, property_longitude);
CREATE INDEX idx_roommate_announcements_move_in_date ON roommate_announcements(move_in_date);
CREATE INDEX idx_roommate_announcements_created_at ON roommate_announcements(created_at);

CREATE INDEX idx_roommate_applications_announcement ON roommate_applications(announcement_id);
CREATE INDEX idx_roommate_applications_applicant ON roommate_applications(applicant_id);
CREATE INDEX idx_roommate_applications_poster ON roommate_applications(poster_id);
CREATE INDEX idx_roommate_applications_status ON roommate_applications(status);
CREATE INDEX idx_roommate_applications_applied_at ON roommate_applications(applied_at);

CREATE INDEX idx_conversations_announcement ON conversations(announcement_id);
CREATE INDEX idx_conversations_updated_at ON conversations(updated_at);

CREATE INDEX idx_messages_conversation ON messages(conversation_id);
CREATE INDEX idx_messages_sender ON messages(sender_id);
CREATE INDEX idx_messages_timestamp ON messages(timestamp);
CREATE INDEX idx_messages_is_read ON messages(is_read);

CREATE INDEX idx_roommate_matches_user ON roommate_matches(user_id);
CREATE INDEX idx_roommate_matches_recommended_user ON roommate_matches(recommended_user_id);
CREATE INDEX idx_roommate_matches_announcement ON roommate_matches(announcement_id);
CREATE INDEX idx_roommate_matches_score ON roommate_matches(compatibility_score); 