ALTER TABLE alert_rules
    ADD COLUMN layout_type VARCHAR(50) NOT NULL DEFAULT 'DEFAULT',
    ADD COLUMN custom_subject VARCHAR(255),
    ADD COLUMN custom_body TEXT;
