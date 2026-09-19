CREATE TABLE workspaces (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    slug       VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE workspace_members (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role         VARCHAR(50) NOT NULL,
    joined_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_workspace_member UNIQUE (workspace_id, user_id)
);
CREATE INDEX idx_member_user_id ON workspace_members(user_id);
CREATE INDEX idx_member_workspace_id ON workspace_members(workspace_id);

INSERT INTO workspaces (id, name, slug)
SELECT 
    gen_random_uuid(), 
    CONCAT(email, '''s Personal Workspace'), 
    REPLACE(REPLACE(email, '@', '-'), '.', '-')
FROM users;

INSERT INTO workspace_members (workspace_id, user_id, role)
SELECT w.id, u.id, 'OWNER'
FROM workspaces w
JOIN users u ON w.slug = REPLACE(REPLACE(u.email, '@', '-'), '.', '-');
