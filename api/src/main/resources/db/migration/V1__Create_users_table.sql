-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'VIEWER',
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index for email lookups
CREATE INDEX idx_users_email ON users(email);

-- Create index for role filtering
CREATE INDEX idx_users_role ON users(role);

-- Insert default admin user
INSERT INTO users (email, password, role, enabled) VALUES
('admin@vault.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewfWuVK1AmKFG7tS', 'ADMIN', true)
ON CONFLICT (email) DO NOTHING;
