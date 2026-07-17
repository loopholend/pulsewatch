ALTER TABLE monitors ADD COLUMN workspace_id UUID REFERENCES workspaces(id) ON DELETE CASCADE;
ALTER TABLE monitors ADD COLUMN created_by UUID REFERENCES users(id);
ALTER TABLE monitors ADD COLUMN last_modified_by UUID REFERENCES users(id);

ALTER TABLE incidents ADD COLUMN workspace_id UUID REFERENCES workspaces(id) ON DELETE CASCADE;
ALTER TABLE alert_rules ADD COLUMN workspace_id UUID REFERENCES workspaces(id) ON DELETE CASCADE;
ALTER TABLE status_pages ADD COLUMN workspace_id UUID REFERENCES workspaces(id) ON DELETE CASCADE;
ALTER TABLE api_keys ADD COLUMN workspace_id UUID REFERENCES workspaces(id) ON DELETE CASCADE;
ALTER TABLE maintenance_windows ADD COLUMN workspace_id UUID REFERENCES workspaces(id) ON DELETE CASCADE;
ALTER TABLE audit_logs ADD COLUMN workspace_id UUID REFERENCES workspaces(id) ON DELETE CASCADE;

UPDATE monitors m SET workspace_id = (SELECT workspace_id FROM workspace_members wm WHERE wm.user_id = m.user_id), created_by = m.user_id, last_modified_by = m.user_id;
UPDATE incidents i SET workspace_id = (SELECT workspace_id FROM monitors m WHERE m.id = i.monitor_id);
UPDATE alert_rules ar SET workspace_id = (SELECT workspace_id FROM monitors m WHERE m.id = ar.monitor_id);
UPDATE status_pages sp SET workspace_id = (SELECT workspace_id FROM workspace_members wm WHERE wm.user_id = sp.user_id);
UPDATE api_keys ak SET workspace_id = (SELECT workspace_id FROM workspace_members wm WHERE wm.user_id = ak.user_id);
UPDATE maintenance_windows mw SET workspace_id = (SELECT workspace_id FROM monitors m WHERE m.id = mw.monitor_id);
UPDATE audit_logs al SET workspace_id = (SELECT workspace_id FROM workspace_members wm WHERE wm.user_id = al.user_id);

ALTER TABLE monitors ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE incidents ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE alert_rules ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE status_pages ALTER COLUMN workspace_id SET NOT NULL;
ALTER TABLE api_keys ALTER COLUMN workspace_id SET NOT NULL;
