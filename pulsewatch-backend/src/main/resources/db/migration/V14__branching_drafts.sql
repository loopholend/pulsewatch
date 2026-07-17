CREATE TABLE monitor_drafts (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    monitor_id        UUID NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    workspace_id      UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name              VARCHAR(255) NOT NULL,
    url               VARCHAR(500) NOT NULL,
    method            VARCHAR(10) NOT NULL,
    headers_json      TEXT,
    assertions_json   TEXT,
    interval_seconds  INTEGER NOT NULL,
    timeout_ms        INTEGER NOT NULL,
    expected_status   INTEGER NOT NULL,
    notes             TEXT,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by  UUID REFERENCES users(id),
    CONSTRAINT uq_monitor_draft UNIQUE (monitor_id)
);

CREATE TABLE monitor_timeline_events (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    monitor_id    UUID NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    workspace_id  UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    event_type    VARCHAR(50) NOT NULL,
    message       TEXT NOT NULL,
    triggered_by  UUID REFERENCES users(id),
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE workspace_activities (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id  UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    triggered_by  UUID NOT NULL REFERENCES users(id),
    action_type   VARCHAR(50) NOT NULL,
    description   TEXT NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);
