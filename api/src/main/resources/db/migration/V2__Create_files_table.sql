-- Create file_metadata table
CREATE TABLE IF NOT EXISTS file_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    size BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_hash VARCHAR(64) NOT NULL UNIQUE,
    storage_path VARCHAR(500) NOT NULL,
    owner_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    tags TEXT[] DEFAULT '{}',
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    
    -- Foreign key constraint
    CONSTRAINT fk_file_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_file_metadata_owner_id ON file_metadata(owner_id);
CREATE INDEX idx_file_metadata_status ON file_metadata(status);
CREATE INDEX idx_file_metadata_content_type ON file_metadata(content_type);
CREATE INDEX idx_file_metadata_created_at ON file_metadata(created_at);
CREATE INDEX idx_file_metadata_file_hash ON file_metadata(file_hash);

-- GIN index for full-text search on tags and description
CREATE INDEX idx_file_metadata_tags ON file_metadata USING GIN(tags);
CREATE INDEX idx_file_metadata_description_search ON file_metadata USING GIN(to_tsvector('english', COALESCE(description, '')));
CREATE INDEX idx_file_metadata_name_search ON file_metadata USING GIN(to_tsvector('english', name));

-- Add check constraints
ALTER TABLE file_metadata ADD CONSTRAINT chk_file_size_positive CHECK (size > 0);
ALTER TABLE file_metadata ADD CONSTRAINT chk_file_status CHECK (status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED'));

-- Update trigger for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_file_metadata_updated_at 
    BEFORE UPDATE ON file_metadata 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();
