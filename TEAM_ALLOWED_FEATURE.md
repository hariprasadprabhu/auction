# Team Allowed Feature Documentation

## Overview
The `teamAllowed` field has been added to the Tournament entity to allow administrators to restrict the maximum number of teams that can be added to a tournament. This field is **database-only** and cannot be accessed or modified through the UI or API endpoints.

## Implementation Details

### 1. Database Schema Changes
- **Entity**: `Tournament.java`
- **Field**: `teamAllowed` (Integer, nullable)
- **Type**: JPA field with no special constraints (allows NULL, meaning no limit)

### 2. API Behavior

#### Reading Tournament Details
- The `teamAllowed` field is now included in all `TournamentResponse` objects
- When calling `/tournaments/{id}` or `/tournaments`, the response will include the `teamAllowed` field
- Example response:
```json
{
  "id": 1,
  "name": "IPL 2024",
  "teamAllowed": 10,
  "totalTeams": 12,
  ...
}
```

#### Adding Teams
- **Endpoint**: `POST /tournaments/{tournamentId}/teams`
- When a team creation request is made, the system checks if the current team count has reached the `teamAllowed` limit
- If the limit is reached, the API returns a **400 Bad Request** error:
```json
{
  "error": "BAD_REQUEST",
  "message": "Reached maximum allowed teams: 10",
  "timestamp": "2024-03-19T10:30:00"
}
```

### 3. Implementation Logic

#### TeamService.create() Method
```java
// Check if teamAllowed limit has been reached
long currentTeamCount = teamRepository.countByTournamentId(tournamentId);
if (tournament.getTeamAllowed() != null && currentTeamCount >= tournament.getTeamAllowed()) {
    throw new IllegalArgumentException("Reached maximum allowed teams: " + tournament.getTeamAllowed());
}
```

**Key Points**:
- Check only happens if `teamAllowed` is NOT NULL
- If `teamAllowed` is NULL, there's no limit (unlimited teams)
- Comparison uses `>=` to allow exactly `teamAllowed` teams

## Database Management

### Setting the Value
The `teamAllowed` field must be set manually in the database:

```sql
-- Set teamAllowed for a specific tournament
UPDATE tournaments SET team_allowed = 10 WHERE id = 1;

-- Remove limit (set to NULL)
UPDATE tournaments SET team_allowed = NULL WHERE id = 1;

-- View current settings
SELECT id, name, team_allowed, (SELECT COUNT(*) FROM teams WHERE tournament_id = tournaments.id) as current_teams 
FROM tournaments;
```

### Database Column Definition
- Column name: `team_allowed`
- Data type: `INTEGER`
- Nullable: YES
- Default: NULL (no limit)

## Files Modified

1. **`Tournament.java`** - Entity
   - Added: `private Integer teamAllowed;`

2. **`TournamentResponse.java`** - DTO
   - Added: `private Integer teamAllowed;`

3. **`TournamentService.java`** - Service
   - Updated `toResponse()` method to include `teamAllowed`

4. **`TeamService.java`** - Service
   - Updated `create()` method to validate against `teamAllowed` limit

## Workflow Example

1. **Admin creates a tournament**
   ```
   Tournament: "IPL 2024"
   Total Teams in DB: 0
   Team Allowed: Not set initially
   ```

2. **Admin manually sets teamAllowed in database**
   ```sql
   UPDATE tournaments SET team_allowed = 8 WHERE id = 1;
   ```

3. **API now enforces the limit**
   - Teams 1-8 can be created successfully
   - Team 9 creation fails with error: "Reached maximum allowed teams: 8"

4. **To increase limit**
   ```sql
   UPDATE tournaments SET team_allowed = 12 WHERE id = 1;
   ```

## Error Handling

- **Exception Type**: `IllegalArgumentException`
- **HTTP Status**: 400 Bad Request
- **Error Code**: `BAD_REQUEST`
- **Message Format**: `"Reached maximum allowed teams: {teamAllowed}"`

The error is handled by the existing `GlobalExceptionHandler.handleBadRequest()` method.

## No Changes to UI/API Input

- The `TournamentRequest` DTO remains unchanged
- Users **cannot** set `teamAllowed` through:
  - Tournament creation API
  - Tournament update API
  - UI forms
  
- The field is **read-only** in API responses (informational only)

## Testing Scenarios

1. **Scenario 1: No limit set**
   - `teamAllowed` = NULL
   - Add 50 teams → All succeed

2. **Scenario 2: Limit set to 5**
   - `teamAllowed` = 5
   - Add teams 1-5 → Success
   - Add team 6 → Error

3. **Scenario 3: Increase limit**
   - `teamAllowed` = 5, have 4 teams
   - Update `teamAllowed` to 10
   - Add team 5-10 → All succeed

## Rollback/Removal

To disable this feature temporarily without code changes:
1. Set all `team_allowed` values to NULL in the database
2. The field will still appear in API responses but won't restrict team creation

To completely remove the feature:
1. Remove `teamAllowed` field from `Tournament.java`
2. Remove from `TournamentResponse.java`
3. Update `TournamentService.toResponse()`
4. Remove the validation check from `TeamService.create()`
5. Drop the column from the database

---

**Version**: 1.0  
**Date**: March 19, 2024  
**Status**: Implemented and Ready for Production

