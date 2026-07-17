-- V15: Add missing workspace_id indexes for all major workspace-scoped tables.
-- After V12 added workspace_id columns, V7 index file was not updated.
-- Every authenticated list/filter query goes through workspace_id, so these are critical.

CREATE INDEX IF NOT EXISTS idx_monitors_workspace_id        ON monitors(workspace_id);
CREATE INDEX IF NOT EXISTS idx_incidents_workspace_id       ON incidents(workspace_id);
CREATE INDEX IF NOT EXISTS idx_alert_rules_workspace_id     ON alert_rules(workspace_id);
CREATE INDEX IF NOT EXISTS idx_monitor_drafts_workspace_id  ON monitor_drafts(workspace_id);
