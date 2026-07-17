-- Phase 6: Maintenance Windows + Assertions

-- Maintenance windows: skip incidents/alerts during planned downtime
CREATE TABLE maintenance_windows (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    monitor_id UUID NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    starts_at  TIMESTAMP NOT NULL,
    ends_at    TIMESTAMP NOT NULL,
    reason     TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_mw_time_order CHECK (ends_at > starts_at)
);
CREATE INDEX idx_mw_monitor_id ON maintenance_windows(monitor_id);
CREATE INDEX idx_mw_time_range  ON maintenance_windows(starts_at, ends_at);

-- Assertions: validate more than just status code
CREATE TABLE monitor_assertions (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    monitor_id  UUID NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    assert_type VARCHAR(50)  NOT NULL, -- STATUS_CODE, BODY_CONTAINS, BODY_NOT_CONTAINS
    operator    VARCHAR(20)  NOT NULL, -- EQ, CONTAINS, NOT_CONTAINS
    expected    VARCHAR(500) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_ma_monitor_id ON monitor_assertions(monitor_id);
