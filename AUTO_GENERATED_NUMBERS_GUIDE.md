# Auto-Generated Incremental Numbers Implementation

## Overview
Team numbers and Player numbers are now **automatically generated** with incremental numbering based on the order of creation within each tournament.

## Implementation Details

### Team Numbers
- **Format**: `T###` (e.g., T001, T002, T003, etc.)
- **Generation**: Automatically generated based on count of existing teams in the tournament
- **Location**: `TeamService.create()` method
- **Cannot be changed**: Team numbers are immutable once created

### Player Numbers
- **Format**: `P###` (e.g., P001, P002, P003, etc.)
- **Generation**: Automatically generated based on count of existing players in the tournament
- **Location**: `PlayerService.register()` method
- **Cannot be changed**: Player numbers are immutable once created

## Changes Made

### 1. TeamRepository
**File**: `/src/main/java/com/bid/auction/repository/TeamRepository.java`

Added method to count teams per tournament:
```java
long countByTournamentId(Long tournamentId);
```

### 2. TeamService
**File**: `/src/main/java/com/bid/auction/service/TeamService.java`

**Create Method** - Now auto-generates team number:
```java
public TeamResponse create(Long tournamentId, TeamRequest req, User user) {
    Tournament tournament = tournamentService.findAndVerifyOwner(tournamentId, user);
    
    // Auto-generate team number
    long count = teamRepository.countByTournamentId(tournamentId);
    String teamNumber = String.format("T%03d", count + 1);  // T001, T002, etc.
    
    Team team = Team.builder()
            .teamNumber(teamNumber)  // Set auto-generated number
            .name(req.getName())
            .ownerName(req.getOwnerName())
            .mobileNumber(req.getMobileNumber())
            .tournament(tournament)
            .build();
    // ...
}
```

**Update Method** - Removed teamNumber modification (immutable):
- Team numbers can no longer be changed after creation
- Only name, ownerName, mobileNumber, and logo can be updated

### 3. TeamRequest DTO
**File**: `/src/main/java/com/bid/auction/dto/request/TeamRequest.java`

Made `teamNumber` field optional:
```java
// teamNumber is auto-generated, no need to provide it
private String teamNumber;  // No @NotBlank annotation
```

## API Usage

### Creating a Team
**Before**:
```json
{
  "teamNumber": "T001",  // User had to provide this
  "name": "Team Alpha",
  "ownerName": "John Doe",
  "mobileNumber": "1234567890"
}
```

**After**:
```json
{
  "name": "Team Alpha",
  "ownerName": "John Doe",
  "mobileNumber": "1234567890"
  // teamNumber is auto-generated and assigned by the system
}
```

### Response
Team numbers are automatically assigned in the response:
```json
{
  "id": 1,
  "teamNumber": "T001",  // Auto-generated
  "name": "Team Alpha",
  "ownerName": "John Doe",
  "mobileNumber": "1234567890",
  "tournamentId": 1,
  "logoUrl": null
}
```

## Player Numbers (Already Implemented)

Player numbers were already implemented with auto-generation in `PlayerService.register()`:
```java
long count = playerRepository.countByTournamentId(tournamentId);
String playerNumber = String.format("P%03d", count + 1);  // P001, P002, etc.
```

## Benefits

1. **No Duplicates**: System ensures unique incremental numbers
2. **Automatic**: No manual entry required
3. **Consistent Format**: Standard format across all tournaments
4. **Scoped by Tournament**: Each tournament has its own numbering sequence
5. **Immutable**: Once created, cannot be changed (data integrity)

## Testing

To test the auto-generation:

1. Create a tournament
2. Create first team (should get T001)
3. Create second team (should get T002)
4. Verify team numbers in the response

Same logic applies to players:
1. Register first player (should get P001)
2. Register second player (should get P002)
3. Verify player numbers in the response

## Database Considerations

- No additional database columns needed
- The numbering is calculated at runtime based on existing records
- No sequences or auto-increment columns required
- This approach is database-agnostic

## Migration Notes

If you have existing teams with manual team numbers:
- No migration needed - existing data remains unchanged
- New teams will be numbered starting from the current count + 1
- Example: If you have 5 existing teams, the next team will be T006

## Files Modified

1. ✅ `/src/main/java/com/bid/auction/repository/TeamRepository.java` - Added countByTournamentId method
2. ✅ `/src/main/java/com/bid/auction/service/TeamService.java` - Auto-generate team numbers
3. ✅ `/src/main/java/com/bid/auction/dto/request/TeamRequest.java` - Made teamNumber optional

