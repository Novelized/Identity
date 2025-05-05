-- Add foreign key constraint to link authorizations to registered clients
ALTER TABLE oauth2_authorization
  ADD CONSTRAINT fk_oauth2_authorization_client
  FOREIGN KEY (registered_client_id)
  REFERENCES oauth2_registered_client(id);

-- Add index on registered_client_id for faster lookups based on client
CREATE INDEX idx_oauth2_authorization_registered_client_id
  ON oauth2_authorization(registered_client_id);

-- Add index on state for faster lookups based on state (e.g., during authorization code flow)
CREATE INDEX idx_oauth2_authorization_state
  ON oauth2_authorization(state); 