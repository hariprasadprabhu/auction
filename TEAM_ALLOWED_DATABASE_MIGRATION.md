# Team Allowed Feature - Database Migration Guide

## For Production Deployment

If you're deploying this feature to production, you'll need to add the new column to your existing database.

### Option 1: Direct SQL (For Manual Migration)

```sql
-- Add the team_allowed column to tournaments table
ALTER TABLE tournaments ADD COLUMN team_allowed INTEGER;

-- Verify the column was added
SELECT * FROM tournaments LIMIT 1;

-- Set initial values (optional - set a default limit for all tournaments)
-- Example: Allow max 10 teams per tournament
-- UPDATE tournaments SET team_allowed = 10 WHERE team_allowed IS NULL;
```

### Option 2: Using Flyway (Recommended for Production)

Create a new migration file: `V{VERSION}__Add_team_allowed_to_tournaments.sql`

```sql
ALTER TABLE tournaments ADD COLUMN team_allowed INTEGER;
```

Example filename: `V8__Add_team_allowed_to_tournaments.sql`

### Option 3: Using Liquibase

Create a new changeset in your `changelog.xml`:

```xml
<changeSet id="8" author="admin">
    <addColumn tableName="tournaments">
        <column name="team_allowed" type="INTEGER"/>
    </addColumn>
</changeSet>
```

## For Docker/Containerized Deployment

If you're using Docker with PostgreSQL (like in your `compose.yaml`):

1. **Update your SQL initialization scripts** (if any):
   - Add the ALTER TABLE statement to your init scripts

2. **Spring Boot will handle it automatically**:
   - Since you're using Spring Boot, the JPA entity definition will ensure the column exists
   - If using Flyway/Liquibase migrations, add the migration file as described above

### Example for Docker Compose

If you have a `sql/` directory mounted in your Docker container:

```sql
-- File: sql/migrations/add_team_allowed.sql
ALTER TABLE tournaments ADD COLUMN IF NOT EXISTS team_allowed INTEGER;
```

## Verification After Migration

After running the migration, verify the setup:

```sql
-- Check if column exists
\d tournaments;
-- OR
DESC tournaments;
-- OR  
SELECT column_name, data_type FROM information_schema.columns 
WHERE table_name = 'tournaments' AND column_name = 'team_allowed';

-- View all tournaments with the new field
SELECT id, name, team_allowed FROM tournaments;

-- Count total teams per tournament
SELECT 
    t.id,
    t.name,
    t.team_allowed,
    (SELECT COUNT(*) FROM teams WHERE tournament_id = t.id) as current_teams
FROM tournaments t;
```

## Rollback Plan (If Needed)

To remove this feature:

```sql
-- Drop the column
ALTER TABLE tournaments DROP COLUMN team_allowed;

-- Verify
SELECT * FROM tournaments LIMIT 1;
```

And remove the field from:
- `Tournament.java` entity
- `TournamentResponse.java` DTO
- `TournamentService.java` (toResponse method)
- `TeamService.java` (validation logic)

## Testing After Migration

Run these tests to ensure the feature works correctly:

```sql
-- Test 1: Create a tournament with team limit
UPDATE tournaments SET team_allowed = 3 WHERE id = 1;

-- Test 2: Add teams and verify count
SELECT 
    t.id, 
    t.name, 
    t.team_allowed,
    (SELECT COUNT(*) FROM teams WHERE tournament_id = t.id) as current_count
FROM tournaments t WHERE id = 1;

-- Expected: When current_count = 3, adding another team should fail with error

-- Test 3: Tournament with no limit
UPDATE tournaments SET team_allowed = NULL WHERE id = 2;
-- Expected: Can add unlimited teams

-- Test 4: Increase limit for existing tournament
UPDATE tournaments SET team_allowed = 5 WHERE id = 1;
-- Expected: Can now add 2 more teams (if current count was 3)
```

## Environment Variables (If Applicable)

No new environment variables are needed for this feature. The `teamAllowed` value is set directly in the database.

## Spring Boot Auto-Schema Generation

If you're using `spring.jpa.hibernate.ddl-auto=update`:
- Spring will automatically add the column to the database
- No manual migration needed in development/test environments

**However**, for production, it's recommended to:
1. Use explicit migration tools (Flyway/Liquibase)
2. Test migrations in a staging environment first
3. Have a rollback plan ready

## Deployment Checklist

- [ ] Database migration script created/prepared
- [ ] Migration tested in development/staging environment
- [ ] Verified column is created with correct data type (INTEGER)
- [ ] Spring Boot application compiled with new entity changes
- [ ] API tested with new field included in responses
- [ ] Initial `team_allowed` values set for existing tournaments (if needed)
- [ ] Error handling verified when team limit is reached
- [ ] Documentation shared with team
- [ ] Rollback plan documented and tested

---

**Note**: If you're using Railway or Render for deployment, the migration will be applied automatically when you deploy the new Spring Boot application, assuming you're using Flyway/Liquibase or relying on Hibernate's schema auto-generation.

