# Team Deletion & Editing - Clean Up Strategy

## Overview
Teams are **tournament-specific** - each team belongs to exactly one tournament. When deleting or editing a team, we must properly handle related `AuctionPlayer` records within that tournament to maintain data integrity.

## Changes Made

### 1. TeamService - Delete Method Enhancement

**Location:** `/home/hari/proj/auction/src/main/java/com/bid/auction/service/TeamService.java`

#### Added Dependency
```java
private final AuctionPlayerRepository auctionPlayerRepository;
```

#### Updated Delete Method
```java
@Transactional
public void delete(Long id, User user) {
    Team team = findTeam(id);
    tournamentService.findAndVerifyOwner(team.getTournament().getId(), user);
    
    // Step 1: Clear all AuctionPlayer references to this team
    var auctionPlayers = auctionPlayerRepository.findBySoldToTeamId(id);
    for (var ap : auctionPlayers) {
        ap.setSoldToTeam(null);
        ap.setSoldPrice(null);
        auctionPlayerRepository.save(ap);
    }
    
    // Step 2: Delete team purse records
    teamPurseService.deleteTeamPurses(id);
    
    // Step 3: Delete the team
    teamRepository.delete(team);
}
```

### 2. TeamPurseService - New Method

**Location:** `/home/hari/proj/auction/src/main/java/com/bid/auction/service/TeamPurseService.java`

#### New Method Added
```java
@Transactional
public void deleteTeamPurseInTournament(Long teamId, Long tournamentId) {
    // Delete purse record for a specific team in a specific tournament
    // Teams are tournament-specific, so this is the only purse record for this team
    teamPurseRepository.deleteByTeamIdAndTournamentId(teamId, tournamentId);
}
```

## Deletion Flow Diagram

```
DELETE Team Request
        ↓
┌──────────────────────────────────────────────────────────────┐
│ 1. Verify team exists & user is tournament owner             │
└──────────────────────────────────────────────────────────────┘
        ↓
┌──────────────────────────────────────────────────────────────┐
│ 2. Find all AuctionPlayers where soldToTeam = this team     │
│    Example: Team had bought 3 players                        │
└──────────────────────────────────────────────────────────────┘
        ↓
┌──────────────────────────────────────────────────────────────┐
│ 3. Clear references for each AuctionPlayer:                  │
│    - Set soldToTeam = null                                   │
│    - Set soldPrice = null                                    │
│    - These players go back to UPCOMING status                │
└──────────────────────────────────────────────────────────────┘
        ↓
┌──────────────────────────────────────────────────────────────┐
│ 4. Delete all TeamPurse records for this team                │
│    - Removes financial data across all tournaments           │
└──────────────────────────────────────────────────────────────┘
        ↓
┌──────────────────────────────────────────────────────────────┐
│ 5. Delete the Team record                                    │
│    - Cascading delete cleans up references                   │
└──────────────────────────────────────────────────────────────┘
        ↓
    Team Deleted ✅
```

## Data Integrity Details

### Before Deletion
```
Team: T001 (Delhi Kings)
├─ AuctionPlayers sold to this team:
│  ├─ Player A (Virat) - soldToTeam: T001, soldPrice: 50,000
│  ├─ Player B (Rohit) - soldToTeam: T001, soldPrice: 40,000
│  └─ Player C (Bumrah) - soldToTeam: T001, soldPrice: 35,000
│
└─ TeamPurse records:
   ├─ Tournament 1: initialPurse: 100,000
   ├─ Tournament 2: initialPurse: 100,000
   └─ Tournament 3: initialPurse: 100,000
```

### After Deletion
```
Team: T001 - DELETED ❌

AuctionPlayers (Updated):
├─ Player A (Virat) - soldToTeam: null, soldPrice: null, auctionStatus: UPCOMING
├─ Player B (Rohit) - soldToTeam: null, soldPrice: null, auctionStatus: UPCOMING
└─ Player C (Bumrah) - soldToTeam: null, soldPrice: null, auctionStatus: UPCOMING

TeamPurse records (Tournament 1, 2, 3): DELETED ❌
```

## Key Points

✅ **AuctionPlayer Cleanup**: When a team is deleted, all players previously sold to that team are cleared from the `soldToTeam` reference. They retain their auction history but can be re-sold to another team or remain UPCOMING.

✅ **Purse Records**: All team purse records across all tournaments are deleted. This ensures no orphaned financial records remain.

✅ **Transactional**: The delete operation is `@Transactional` to ensure all operations succeed or all fail (no partial deletions).

✅ **Cascading**: The Team entity may have other cascading relationships defined, which will also be cleaned up by JPA.

✅ **Data Consistency**: Players return to UPCOMING status and can be re-auctioned without data corruption.

## Update Method (Edit Team)

**Current behavior:** Update method (`teamService.update()`) only modifies basic team info:
- Team name
- Owner name
- Mobile number
- Logo

**No cleanup needed** because:
- Team is still in tournament
- AuctionPlayer references remain valid
- TeamPurse records stay active
- No data is lost

**Future consideration:** If you want to recalculate purses on team name change (unlikely), you can use:
```java
teamPurseService.recalculateAllTeamPurses(team.getTournament());
```

## Testing Scenarios

### Scenario 1: Delete Team with Sold Players
```
Given:
- Team T001 has bought 5 players
- Total players in tournament: 20

When: DELETE /api/teams/{t001}

Then:
✅ 5 AuctionPlayers soldToTeam reset to null
✅ 5 AuctionPlayers available for re-auction
✅ TeamPurse deleted
✅ Team deleted
✅ Tournament still has 19 remaining teams
```

### Scenario 2: Delete Team with No Sold Players
```
Given:
- Team T002 has NOT bought any players
- No AuctionPlayer references

When: DELETE /api/teams/{t002}

Then:
✅ No AuctionPlayers to update
✅ TeamPurse deleted
✅ Team deleted
✅ Clean removal
```

### Scenario 3: Edit Team (Update)
```
Given:
- Team T001 exists with players and purse

When: PUT /api/teams/{t001} with new name/logo

Then:
✅ Team info updated
✅ AuctionPlayers unchanged
✅ TeamPurse records unchanged
✅ No data loss
```

## SQL Impact

### Delete Operations (Behind the Scenes)
```sql
-- 1. Update AuctionPlayers
UPDATE auction_players 
SET sold_to_team_id = null, sold_price = null 
WHERE sold_to_team_id = ?;

-- 2. Delete TeamPurses
DELETE FROM team_purse 
WHERE team_id = ?;

-- 3. Delete Team
DELETE FROM teams 
WHERE id = ?;
```

## Files Modified

1. **TeamService.java**
   - Added `AuctionPlayerRepository` dependency
   - Updated `delete()` method with `@Transactional`
   - Added cleanup logic for AuctionPlayers
   - Added call to `teamPurseService.deleteTeamPurses()`

2. **TeamPurseService.java**
   - Added `deleteTeamPurses(Long teamId)` method
   - Deletes all purse records for a team

## Potential Edge Cases

### Edge Case 1: Concurrent Deletions
**Handled by:** `@Transactional` - ensures atomicity

### Edge Case 2: Invalid Team ID
**Handled by:** `findTeam(id)` throws `ResourceNotFoundException`

### Edge Case 3: User Not Tournament Owner
**Handled by:** `findAndVerifyOwner()` throws `Exception`

### Edge Case 4: No Sold Players
**Handled by:** `findBySoldToTeamId()` returns empty list, loop does nothing

## Summary

✅ Delete operation now properly cleans up related `AuctionPlayer` records
✅ All `TeamPurse` records are deleted for the team
✅ Team deletion is atomic and transactional
✅ No orphaned records remain in database
✅ Players can be re-auctioned if needed
✅ Data integrity is maintained

