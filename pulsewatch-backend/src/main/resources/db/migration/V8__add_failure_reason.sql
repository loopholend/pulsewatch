ALTER TABLE monitor_results ADD COLUMN failure_reason VARCHAR(255);
ALTER TABLE incidents ADD COLUMN failure_reason VARCHAR(255);
