-- V6: Create connection requests table for student-to-student connections
-- This enables students to send connection requests to each other for roommate matching

CREATE TABLE connection_requests (
    id BIGSERIAL PRIMARY KEY,
    sender_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'ACCEPTED', 'REJECTED'
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    responded_at TIMESTAMP,
    response_message TEXT,
    
    -- Prevent duplicate requests between same users
    UNIQUE(sender_id, receiver_id),
    
    -- Prevent self-requests
    CHECK (sender_id != receiver_id)
);

-- Create indexes for better query performance
CREATE INDEX idx_connection_requests_sender ON connection_requests(sender_id);
CREATE INDEX idx_connection_requests_receiver ON connection_requests(receiver_id);
CREATE INDEX idx_connection_requests_status ON connection_requests(status);
CREATE INDEX idx_connection_requests_created_at ON connection_requests(created_at);

-- Add comments for documentation
COMMENT ON TABLE connection_requests IS 'Stores connection requests between students for roommate matching';
COMMENT ON COLUMN connection_requests.sender_id IS 'ID of the student sending the connection request';
COMMENT ON COLUMN connection_requests.receiver_id IS 'ID of the student receiving the connection request';
COMMENT ON COLUMN connection_requests.message IS 'Optional message from sender to receiver';
COMMENT ON COLUMN connection_requests.status IS 'Status of the request: PENDING, ACCEPTED, REJECTED';
COMMENT ON COLUMN connection_requests.response_message IS 'Optional response message when accepting/rejecting'; 