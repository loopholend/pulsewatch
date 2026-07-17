-- V16: Add ON DELETE CASCADE to foreign keys in incident_events and alert_history
-- This ensures deleting a monitor or incident cleanly cascades without constraint violations.

ALTER TABLE incident_events
  DROP CONSTRAINT IF EXISTS incident_events_incident_id_fkey;

ALTER TABLE incident_events
  ADD CONSTRAINT fk_incident_events_incident
  FOREIGN KEY (incident_id) REFERENCES incidents(id) ON DELETE CASCADE;

ALTER TABLE alert_history
  DROP CONSTRAINT IF EXISTS alert_history_monitor_id_fkey;

ALTER TABLE alert_history
  ADD CONSTRAINT fk_alert_history_monitor
  FOREIGN KEY (monitor_id) REFERENCES monitors(id) ON DELETE CASCADE;
