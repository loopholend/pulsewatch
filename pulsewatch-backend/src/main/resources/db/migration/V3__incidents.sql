-- V3: Add missing columns to incidents (already created by V1 without status and with wrong types)
ALTER TABLE incidents
    ADD COLUMN IF NOT EXISTS status VARCHAR(20);

-- Set default status for existing rows
UPDATE incidents SET status = 'OPEN' WHERE status IS NULL AND resolved_at IS NULL;
UPDATE incidents SET status = 'RESOLVED' WHERE status IS NULL AND resolved_at IS NOT NULL;

-- Make status NOT NULL after backfill
ALTER TABLE incidents
    ALTER COLUMN status SET NOT NULL;

-- Change duration_seconds from INT to BIGINT if needed (safe cast)
ALTER TABLE incidents
    ALTER COLUMN duration_seconds TYPE BIGINT USING duration_seconds::BIGINT;

-- Add created_at / updated_at if they don't exist
ALTER TABLE incidents
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();
