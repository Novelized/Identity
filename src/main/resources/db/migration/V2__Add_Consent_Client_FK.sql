-- Add foreign key constraint to link consent records to registered clients
-- Ensures that consents are only stored for valid, existing clients.
ALTER TABLE oauth2_authorization_consent
  ADD CONSTRAINT fk_oauth2_authorization_consent_client
  FOREIGN KEY (registered_client_id)
  REFERENCES oauth2_registered_client(id); 