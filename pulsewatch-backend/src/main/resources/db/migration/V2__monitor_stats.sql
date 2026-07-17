-- V2: Add missing columns to monitor_stats (already created by V1 without these columns)
ALTER TABLE monitor_stats
    ADD COLUMN IF NOT EXISTS total_checks BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS successful_checks BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS failed_checks BIGINT NOT NULL DEFAULT 0;

-- Add NOT NULL defaults to columns that V1 created as nullable
ALTER TABLE monitor_stats
    ALTER COLUMN uptime SET DEFAULT 0,
    ALTER COLUMN avg_latency SET DEFAULT 0,
    ALTER COLUMN p95_latency SET DEFAULT 0,
    ALTER COLUMN error_rate SET DEFAULT 0;

UPDATE monitor_stats SET uptime = 0 WHERE uptime IS NULL;
UPDATE monitor_stats SET avg_latency = 0 WHERE avg_latency IS NULL;
UPDATE monitor_stats SET p95_latency = 0 WHERE p95_latency IS NULL;
UPDATE monitor_stats SET error_rate = 0 WHERE error_rate IS NULL;
