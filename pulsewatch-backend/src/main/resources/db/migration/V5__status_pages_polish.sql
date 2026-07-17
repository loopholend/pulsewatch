-- Phase 5: Status Pages polish
-- Add missing columns to existing tables

ALTER TABLE status_pages
    ADD COLUMN IF NOT EXISTS description TEXT;

ALTER TABLE service_status
    ADD COLUMN IF NOT EXISTS service_name VARCHAR(255);

-- Populate service_name from linked monitor name for existing rows
UPDATE service_status ss
SET service_name = m.name
FROM monitors m
WHERE ss.monitor_id = m.id
  AND ss.service_name IS NULL;

-- Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_service_status_status_page_id ON service_status(status_page_id);
CREATE INDEX IF NOT EXISTS idx_service_status_monitor_id ON service_status(monitor_id);
CREATE INDEX IF NOT EXISTS idx_incidents_monitor_status ON incidents(monitor_id, status);

-- Add unique constraint: one entry per monitor per status page
ALTER TABLE service_status
    ADD CONSTRAINT uq_service_status_page_monitor UNIQUE (status_page_id, monitor_id);
