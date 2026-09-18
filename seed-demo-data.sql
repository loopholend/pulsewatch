-- PulseWatch Seed SQL Script (Idempotent PostgreSQL seed data generator)

-- 1. Demo Admin User
INSERT INTO users (id, email, password_hash, role, created_at, updated_at)
VALUES (
    '11111111-1111-1111-1111-111111111111'::uuid,
    'admin@pulsewatch.dev',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LjMq8hXmNrO',
    'USER',
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;

-- 2. Workspace
INSERT INTO workspaces (id, name, slug, created_at)
VALUES (
    '22222222-2222-2222-2222-222222222222'::uuid,
    'PulseWatch Demo',
    'pulsewatch-demo',
    NOW()
) ON CONFLICT (slug) DO NOTHING;

-- 3. Link User as OWNER of Workspace
INSERT INTO workspace_members (id, workspace_id, user_id, role, joined_at)
SELECT gen_random_uuid(), w.id, u.id, 'OWNER', NOW()
FROM workspaces w, users u
WHERE w.slug = 'pulsewatch-demo' AND u.email = 'admin@pulsewatch.dev'
ON CONFLICT (workspace_id, user_id) DO NOTHING;

-- 4. Insert 5 Monitors
INSERT INTO monitors (
    id, workspace_id, created_by, last_modified_by,
    name, url, monitor_type, method, expected_status,
    interval_seconds, timeout_ms, active, current_status,
    consecutive_failures, created_at, updated_at
) VALUES
    ('33333333-3333-3333-3333-333333333301'::uuid, '22222222-2222-2222-2222-222222222222'::uuid, '11111111-1111-1111-1111-111111111111'::uuid, '11111111-1111-1111-1111-111111111111'::uuid, 'Google', 'https://www.google.com', 'HTTP', 'GET', 200, 60, 5000, true, 'UP', 0, NOW(), NOW()),
    ('33333333-3333-3333-3333-333333333302'::uuid, '22222222-2222-2222-2222-222222222222'::uuid, '11111111-1111-1111-1111-111111111111'::uuid, '11111111-1111-1111-1111-111111111111'::uuid, 'GitHub', 'https://github.com', 'HTTP', 'GET', 200, 60, 5000, true, 'UP', 0, NOW(), NOW()),
    ('33333333-3333-3333-3333-333333333303'::uuid, '22222222-2222-2222-2222-222222222222'::uuid, '11111111-1111-1111-1111-111111111111'::uuid, '11111111-1111-1111-1111-111111111111'::uuid, 'Cloudflare', 'https://www.cloudflare.com', 'HTTP', 'GET', 200, 60, 5000, true, 'UP', 0, NOW(), NOW()),
    ('33333333-3333-3333-3333-333333333304'::uuid, '22222222-2222-2222-2222-222222222222'::uuid, '11111111-1111-1111-1111-111111111111'::uuid, '11111111-1111-1111-1111-111111111111'::uuid, 'JSONPlaceholder API', 'https://jsonplaceholder.typicode.com/posts/1', 'HTTP', 'GET', 200, 60, 5000, true, 'UP', 0, NOW(), NOW()),
    ('33333333-3333-3333-3333-333333333305'::uuid, '22222222-2222-2222-2222-222222222222'::uuid, '11111111-1111-1111-1111-111111111111'::uuid, '11111111-1111-1111-1111-111111111111'::uuid, 'HTTPBin', 'https://httpbin.org/get', 'HTTP', 'GET', 200, 60, 5000, true, 'UP', 0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 5. Insert Monitor Results (Last 7 days, 60s interval = ~10,080 rows per monitor)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM monitor_results 
        WHERE monitor_id = '33333333-3333-3333-3333-333333333301'::uuid 
        AND checked_at >= NOW() - INTERVAL '7 days 1 hour'
    ) THEN
        -- Google, GitHub, Cloudflare (99.5% success, 50-300ms)
        INSERT INTO monitor_results (id, monitor_id, status_code, response_time_ms, success, failure_reason, checked_at)
        SELECT 
            gen_random_uuid(), m.id,
            CASE WHEN is_success THEN 200 ELSE NULL END,
            CASE WHEN is_success THEN (50 + floor(random() * 251))::int ELSE 5000 END,
            is_success,
            CASE WHEN is_success THEN NULL ELSE 'Timeout' END,
            t
        FROM (
            SELECT id FROM monitors WHERE id IN (
                '33333333-3333-3333-3333-333333333301'::uuid,
                '33333333-3333-3333-3333-333333333302'::uuid,
                '33333333-3333-3333-3333-333333333303'::uuid
            )
        ) m
        CROSS JOIN generate_series(NOW() - INTERVAL '7 days', NOW(), INTERVAL '60 seconds') AS t
        CROSS JOIN LATERAL (SELECT (random() < 0.995) AS is_success) rnd;

        -- JSONPlaceholder API (98% success, 200-800ms)
        INSERT INTO monitor_results (id, monitor_id, status_code, response_time_ms, success, failure_reason, checked_at)
        SELECT 
            gen_random_uuid(), '33333333-3333-3333-3333-333333333304'::uuid,
            CASE WHEN is_success THEN 200 ELSE NULL END,
            CASE WHEN is_success THEN (200 + floor(random() * 601))::int ELSE 5000 END,
            is_success,
            CASE WHEN is_success THEN NULL ELSE 'Timeout' END,
            t
        FROM generate_series(NOW() - INTERVAL '7 days', NOW(), INTERVAL '60 seconds') AS t
        CROSS JOIN LATERAL (SELECT (random() < 0.98) AS is_success) rnd;

        -- HTTPBin (97% success, 100-500ms)
        INSERT INTO monitor_results (id, monitor_id, status_code, response_time_ms, success, failure_reason, checked_at)
        SELECT 
            gen_random_uuid(), '33333333-3333-3333-3333-333333333305'::uuid,
            CASE WHEN is_success THEN 200 ELSE NULL END,
            CASE WHEN is_success THEN (100 + floor(random() * 401))::int ELSE 5000 END,
            is_success,
            CASE WHEN is_success THEN NULL ELSE 'Timeout' END,
            t
        FROM generate_series(NOW() - INTERVAL '7 days', NOW(), INTERVAL '60 seconds') AS t
        CROSS JOIN LATERAL (SELECT (random() < 0.97) AS is_success) rnd;
    END IF;
END $$;

-- 6. Insert Monitor Stats
INSERT INTO monitor_stats (
    monitor_id, uptime, avg_latency, p95_latency, error_rate,
    total_checks, successful_checks, failed_checks, updated_at
)
SELECT 
    monitor_id,
    ROUND((COUNT(*) FILTER (WHERE success = TRUE)::numeric / COUNT(*)::numeric) * 100, 2)::double precision AS uptime,
    ROUND(AVG(response_time_ms) FILTER (WHERE success = TRUE))::int AS avg_latency,
    PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY response_time_ms)::int AS p95_latency,
    ROUND((COUNT(*) FILTER (WHERE success = FALSE)::numeric / COUNT(*)::numeric) * 100, 2)::double precision AS error_rate,
    COUNT(*) AS total_checks,
    COUNT(*) FILTER (WHERE success = TRUE) AS successful_checks,
    COUNT(*) FILTER (WHERE success = FALSE) AS failed_checks,
    NOW() AS updated_at
FROM monitor_results
WHERE monitor_id IN (
    '33333333-3333-3333-3333-333333333301'::uuid,
    '33333333-3333-3333-3333-333333333302'::uuid,
    '33333333-3333-3333-3333-333333333303'::uuid,
    '33333333-3333-3333-3333-333333333304'::uuid,
    '33333333-3333-3333-3333-333333333305'::uuid
)
GROUP BY monitor_id
ON CONFLICT (monitor_id) DO UPDATE SET
    uptime = EXCLUDED.uptime,
    avg_latency = EXCLUDED.avg_latency,
    p95_latency = EXCLUDED.p95_latency,
    error_rate = EXCLUDED.error_rate,
    total_checks = EXCLUDED.total_checks,
    successful_checks = EXCLUDED.successful_checks,
    failed_checks = EXCLUDED.failed_checks,
    updated_at = EXCLUDED.updated_at;

-- 7. Insert Alert Rules (One per monitor)
DO $$
DECLARE
    v_has_recipient BOOLEAN;
    v_has_name BOOLEAN;
    v_has_notification_email BOOLEAN;
BEGIN
    SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'alert_rules' AND column_name = 'recipient') INTO v_has_recipient;
    SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'alert_rules' AND column_name = 'name') INTO v_has_name;
    SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'alert_rules' AND column_name = 'notification_email') INTO v_has_notification_email;

    IF v_has_recipient AND v_has_name AND NOT v_has_notification_email THEN
        EXECUTE '
            INSERT INTO alert_rules (id, monitor_id, workspace_id, name, alert_type, recipient, severity, enabled, created_at, updated_at)
            SELECT gen_random_uuid(), m.id, m.workspace_id, ''Email Alert - '' || m.name, ''EMAIL'', ''pranjalpal05@gmail.com'', ''CRITICAL'', TRUE, NOW(), NOW()
            FROM monitors m
            WHERE m.workspace_id = ''22222222-2222-2222-2222-222222222222''::uuid
            AND NOT EXISTS (SELECT 1 FROM alert_rules ar WHERE ar.monitor_id = m.id);
        ';
    ELSIF v_has_notification_email THEN
        EXECUTE '
            INSERT INTO alert_rules (id, monitor_id, workspace_id, rule_type, notification_email, cooldown_minutes, enabled, layout_type, created_at, updated_at)
            SELECT gen_random_uuid(), m.id, m.workspace_id, ''INCIDENT_OPENED'', ''pranjalpal05@gmail.com'', 30, TRUE, ''DEFAULT'', NOW(), NOW()
            FROM monitors m
            WHERE m.workspace_id = ''22222222-2222-2222-2222-222222222222''::uuid
            AND NOT EXISTS (SELECT 1 FROM alert_rules ar WHERE ar.monitor_id = m.id);
        ';
    ELSE
        EXECUTE '
            INSERT INTO alert_rules (id, monitor_id, workspace_id, enabled, created_at, updated_at)
            SELECT gen_random_uuid(), m.id, m.workspace_id, TRUE, NOW(), NOW()
            FROM monitors m
            WHERE m.workspace_id = ''22222222-2222-2222-2222-222222222222''::uuid
            AND NOT EXISTS (SELECT 1 FROM alert_rules ar WHERE ar.monitor_id = m.id);
        ';
    END IF;
END $$;
