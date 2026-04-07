-- Ensure admin_actions.metadata is always JSONB even on drifted databases
ALTER TABLE admin_actions
    ALTER COLUMN metadata TYPE jsonb
    USING CASE
        WHEN metadata IS NULL THEN '{}'::jsonb
        ELSE metadata::jsonb
    END;

ALTER TABLE admin_actions
    ALTER COLUMN metadata SET DEFAULT '{}'::jsonb;
