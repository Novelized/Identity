-- Add an index to the client_id column for faster lookups
CREATE INDEX idx_oauth2_registered_client_client_id ON oauth2_registered_client(client_id); 