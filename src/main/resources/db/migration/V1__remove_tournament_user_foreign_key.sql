DO $$
DECLARE
    foreign_key RECORD;
BEGIN
    IF to_regclass('public.tournaments') IS NOT NULL
       AND to_regclass('public.users') IS NOT NULL THEN
        FOR foreign_key IN
            SELECT conname
            FROM pg_constraint
            WHERE conrelid = 'public.tournaments'::regclass
              AND confrelid = 'public.users'::regclass
              AND contype = 'f'
        LOOP
            EXECUTE format(
                'ALTER TABLE public.tournaments DROP CONSTRAINT %I',
                foreign_key.conname
            );
        END LOOP;
    END IF;
END $$;
