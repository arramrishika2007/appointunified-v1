-- V15__feature_a_chat_messages.sql
-- FEATURE A: One-to-One Chat between User and Professional

CREATE TABLE chat_messages (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  appointment_id UUID REFERENCES appointments(id) NOT NULL,
  sender_id UUID REFERENCES users(id) NOT NULL,
  receiver_id UUID REFERENCES users(id) NOT NULL,
  message TEXT NOT NULL,
  is_read BOOLEAN DEFAULT FALSE,
  sent_at TIMESTAMPTZ DEFAULT NOW(),
  filtered BOOLEAN DEFAULT FALSE,  -- True if message contains sensitive data and was blocked
  filter_reason TEXT  -- Reason for filtering if applicable
);

CREATE INDEX idx_chat_messages_appointment ON chat_messages(appointment_id);
CREATE INDEX idx_chat_messages_sender ON chat_messages(sender_id);
CREATE INDEX idx_chat_messages_receiver ON chat_messages(receiver_id);
CREATE INDEX idx_chat_messages_unread ON chat_messages(receiver_id, is_read);
CREATE INDEX idx_chat_messages_sent_at ON chat_messages(sent_at DESC);
