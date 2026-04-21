DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'incidents'
    ) THEN
        ALTER TABLE incidents DROP CONSTRAINT IF EXISTS incidents_status_check;

        ALTER TABLE incidents
            ADD CONSTRAINT incidents_status_check
            CHECK (status IN ('ACTIVE', 'INVESTIGATING', 'INVESTIGATED', 'RESOLVED'));
    END IF;
END
$$;
