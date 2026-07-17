-- V4: Add missing columns to alert_rules and create new Phase 4 tables

-- alert_rules already exists from V1, just add the new columns
ALTER TABLE alert_rules
    ADD COLUMN IF NOT EXISTS notification_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS last_alert_sent_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS rule_type VARCHAR(50);

-- Backfill rule_type for any existing rows (V1 had no rule_type)
UPDATE alert_rules SET rule_type = 'INCIDENT_OPENED' WHERE rule_type IS NULL;

-- Remove old V1 columns that don't exist in new entity (threshold was in V1)
-- We keep them to avoid breaking anything, just mark notification_email as NOT NULL
UPDATE alert_rules SET notification_email = '' WHERE notification_email IS NULL;
ALTER TABLE alert_rules ALTER COLUMN notification_email SET NOT NULL;
ALTER TABLE alert_rules ALTER COLUMN rule_type SET NOT NULL;

-- Create incident_events (new in Phase 4)
CREATE TABLE IF NOT EXISTS incident_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    incident_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    FOREIGN KEY (incident_id) REFERENCES incidents(id)
);

-- Create alert_history (new in Phase 4)
CREATE TABLE IF NOT EXISTS alert_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    incident_id UUID,
    monitor_id UUID NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT NOW(),
    status VARCHAR(20) NOT NULL,
    FOREIGN KEY (monitor_id) REFERENCES monitors(id)
);
