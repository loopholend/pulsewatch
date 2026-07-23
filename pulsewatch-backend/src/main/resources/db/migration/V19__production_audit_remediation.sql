-- V19: Production Audit Remediation
-- Generated: July 2026 — Principal DB Engineer audit
-- All statements use IF NOT EXISTS / safe ON DELETE semantics

-- 1. Missing workspace indexes (tenant isolation)
CREATE INDEX IF NOT EXISTS idx_status_pages_workspace_id            ON status_pages(workspace_id);
CREATE INDEX IF NOT EXISTS idx_api_keys_workspace_id                ON api_keys(workspace_id);
CREATE INDEX IF NOT EXISTS idx_maintenance_windows_workspace_id     ON maintenance_windows(workspace_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_workspace_id              ON audit_logs(workspace_id);
CREATE INDEX IF NOT EXISTS idx_monitor_timeline_events_workspace_id ON monitor_timeline_events(workspace_id);
CREATE INDEX IF NOT EXISTS idx_workspace_activities_workspace_id    ON workspace_activities(workspace_id);

-- 2. Add created_by to api_keys for proper user attribution
ALTER TABLE api_keys ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id) ON DELETE SET NULL;

-- 3. Case-insensitive unique indexes
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_lower       ON users (LOWER(email));
CREATE UNIQUE INDEX IF NOT EXISTS idx_workspaces_slug_lower   ON workspaces (LOWER(slug));
CREATE UNIQUE INDEX IF NOT EXISTS idx_status_pages_slug_lower ON status_pages (LOWER(slug));

-- 4. Missing FK index on refresh_tokens.user_id
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- 5. Composite indexes for hot query paths
CREATE INDEX IF NOT EXISTS idx_monitors_workspace_active              ON monitors(workspace_id, active) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_incidents_workspace_status_started     ON incidents(workspace_id, status, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_alert_rules_eval                       ON alert_rules(monitor_id, enabled, is_verified);
CREATE INDEX IF NOT EXISTS idx_workspace_activities_workspace_created ON workspace_activities(workspace_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_workspace_created           ON audit_logs(workspace_id, created_at DESC);

-- 6. Expiration indexes for cleanup jobs
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at        ON refresh_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_api_keys_expires_at              ON api_keys(expires_at);
CREATE INDEX IF NOT EXISTS idx_workspace_invitations_expires_at ON workspace_invitations(expires_at);
CREATE INDEX IF NOT EXISTS idx_monitor_timeline_events_monitor  ON monitor_timeline_events(monitor_id);

-- 7. Fix ON DELETE for user FK references (allow user deletion)
ALTER TABLE workspace_activities ALTER COLUMN triggered_by DROP NOT NULL;

-- 8. Soft-delete aware partial index
CREATE INDEX IF NOT EXISTS idx_monitors_active_only ON monitors(workspace_id, id) WHERE deleted_at IS NULL;

-- 9. NOT NULL where semantically required
ALTER TABLE workspace_members ALTER COLUMN status SET NOT NULL;
