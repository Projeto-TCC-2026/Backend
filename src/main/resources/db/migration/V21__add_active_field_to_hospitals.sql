-- Add active field to hospitals table
ALTER TABLE hospitals ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

-- Create index for performance on active queries
CREATE INDEX idx_hospitals_active ON hospitals(active);

-- Update all existing hospitals to be active
UPDATE hospitals SET active = TRUE WHERE active IS NULL;