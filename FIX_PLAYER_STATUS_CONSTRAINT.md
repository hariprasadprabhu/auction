# Fix: Player Status CHECK Constraint

## Problem
The database had a CHECK constraint on the `players.status` column that only allowed `PENDING`, `APPROVED`, and `REJECTED` values. When trying to update a player's status to `SOLD` or `UNSOLD`, the database throws a constraint violation error:

```
ERROR: new row for relation "players" violates check constraint "players_status_check"
```

## Solution
Updated `Player.java` entity to include the columnDefinition for the status field with all allowed enum values.

### Code Change
In `src/main/java/com/bid/auction/entity/Player.java`:

```java
@Enumerated(EnumType.STRING)
@Column(columnDefinition = "varchar(255) check (status in ('PENDING', 'APPROVED', 'REJECTED', 'SOLD', 'UNSOLD'))")
@Builder.Default
private PlayerStatus status = PlayerStatus.PENDING;
```

## Manual Database Fix Required

Since the application uses `ddl-auto: update`, Hibernate won't automatically update existing constraints. You need to manually run this SQL on your database:

```sql
-- Drop the old check constraint
ALTER TABLE players DROP CONSTRAINT IF EXISTS players_status_check;

-- Add the new check constraint that includes SOLD and UNSOLD
ALTER TABLE players ADD CONSTRAINT players_status_check 
CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'SOLD', 'UNSOLD'));
```

### How to Execute

**Option 1: Using psql CLI**
```bash
psql -h <your_host> -U <your_username> -d <your_database> -f /tmp/fix_constraint.sql
```

**Option 2: Using a database GUI (DBeaver, pgAdmin, etc.)**
1. Copy the SQL above
2. Run it in your database client

**Option 3: If using Docker**
```bash
docker exec <postgres_container_id> psql -U <username> -d <database> -c "ALTER TABLE players DROP CONSTRAINT IF EXISTS players_status_check; ALTER TABLE players ADD CONSTRAINT players_status_check CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'SOLD', 'UNSOLD'));"
```

## After the Fix
- The sell request will now work correctly
- Players can be updated with `SOLD` and `UNSOLD` statuses without constraint violations
- Future deployments will maintain the correct constraint definition

## Related Files
- `src/main/java/com/bid/auction/entity/Player.java` - Updated entity definition
- `src/main/java/com/bid/auction/enums/PlayerStatus.java` - Enum with SOLD and UNSOLD values

