# Auction Reset API Endpoints Documentation

## Overview
Two new API endpoints have been added to handle auction reset operations:
1. **Reset Specific Auction Players** - Reset individual players in an auction
2. **Reset Entire Auction** - Reset the entire auction and reload all approved players

---

## 1. Reset Specific Auction Players

### Endpoint
```
POST /tournaments/{tournamentId}/auction-players/reset
```

### Description
Resets specific auction players in a tournament. For each player:
- If the player status is **APPROVED**: Makes them available for auction (sets status to UPCOMING)
- If the player status is **NOT APPROVED**: Removes them from auction (sets status to UNSOLD)
- **Always refunds** any sold price back to the team that bought them
- **Recalculates** team purse, maxBid, reserved amount for affected teams

### Authentication
Required - User must own the tournament

### Request Body
```json
{
  "playerIds": [1, 2, 3, 4]
}
```

### Request Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| playerIds | List<Long> | Yes | List of auction player IDs to reset |

### Response
```json
{
  "processedCount": 4,
  "skippedCount": 0,
  "totalRequested": 4
}
```

### Response Fields
| Field | Type | Description |
|-------|------|-------------|
| processedCount | int | Number of players successfully processed |
| skippedCount | int | Number of players skipped (not found or wrong tournament) |
| totalRequested | int | Total number of players requested to process |

### Example cURL
```bash
curl -X POST http://localhost:8080/tournaments/1/auction-players/reset \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "playerIds": [10, 11, 12]
  }'
```

### HTTP Status Codes
- **200 OK** - Success
- **401 Unauthorized** - Invalid or missing authentication
- **403 Forbidden** - User does not own the tournament
- **404 Not Found** - Tournament not found
- **400 Bad Request** - Invalid request body

---

### Reset Entire Auction

### Endpoint
```
POST /tournaments/{tournamentId}/auction/reset-entire
```

### Description
Performs a complete reset of the auction for a tournament:
1. **Refunds all sold players** - Returns sold prices to teams that bought them
2. **Deletes all auction players** - Clears existing auction entries
3. **Re-adds all approved players** - Reloads all players with APPROVED status
4. **Resets team purses** - Recalculates purse, maxBid, reserved amount, and available funds for all teams
5. **Applies current tournament settings** - Any changes to base price, players per team, or purse amount are reflected

This is useful for starting a fresh/re-auction of a tournament while keeping all player registration data intact.

#### Dynamic Settings Recalculation
When the entire auction is reset, the system **automatically picks up the current tournament settings**:

- **Base Price Changes**: Any update to tournament `basePrice` will be applied to all re-added players
- **Players Per Team Changes**: Any update to `playersPerTeam` will affect:
  - `reservedFund` calculation = (remainingSlots - 1) × basePrice
  - `maxBidPerPlayer` = currentPurse - reservedFund
  - Total player slots available per team
- **Purse Amount Changes**: Any update to `purseAmount` will reset each team's:
  - `initialPurse` = new purseAmount
  - `currentPurse` = new purseAmount
  - `maxBidPerPlayer` based on new purse
  - `availableForBidding` based on new purse and reserved fund

The response includes `appliedTournamentSettings` to confirm which settings were used.

### Authentication
Required - User must own the tournament

### Request Body
None (empty POST)

### Response
```json
{
  "deletedAuctionPlayers": 45,
  "readdedApprovedPlayers": 45,
  "teamsReset": 10,
  "appliedTournamentSettings": {
    "purseAmount": 5000000,
    "playersPerTeam": 11,
    "basePrice": 50000
  }
}
```

### Response Fields
| Field | Type | Description |
|-------|------|-------------|
| deletedAuctionPlayers | int | Number of auction players deleted from the previous auction |
| readdedApprovedPlayers | int | Number of approved players re-added to the new auction |
| teamsReset | int | Number of teams whose purses were recalculated |
| appliedTournamentSettings | object | Current tournament settings used for recalculation |
| appliedTournamentSettings.purseAmount | long | Purse amount per team (currently applied) |
| appliedTournamentSettings.playersPerTeam | int | Number of players allowed per team (currently applied) |
| appliedTournamentSettings.basePrice | long | Base price per player (currently applied) |

### Example cURL
```bash
curl -X POST http://localhost:8080/tournaments/1/auction/reset-entire \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### HTTP Status Codes
- **200 OK** - Success
- **401 Unauthorized** - Invalid or missing authentication
- **403 Forbidden** - User does not own the tournament
- **404 Not Found** - Tournament not found

---

## Business Logic Details

### Reset Specific Players

**Process for each auction player:**

1. **Check Registration Status**
   - If the player has an approved registration → Set to UPCOMING (available for auction)
   - If the player has no registration or is REJECTED → Set to UNSOLD (unavailable)
   - If the player is PENDING → Set to UNSOLD (not yet approved)

2. **Handle Sold Players**
   - If the player was already sold (has `soldToTeam` and `soldPrice`):
     - Deduct the `soldPrice` from the team's `purseUsed`
     - Add back to `currentPurse` = `initialPurse` - `purseUsed`
     - Decrement `playersBought` count
     - Increment `remainingSlots`
     - Recalculate `maxBidPerPlayer` and `reservedFund`

3. **Clear Auction Status**
   - Set `soldToTeam = null`
   - Set `soldPrice = null`

### Reset Entire Auction

**Complete flow:**

1. **Refund All Sold Players**
   - For each auction player that was sold:
     - Refund the team using the same logic as above

2. **Delete All Auction Entries**
   - Remove all auction players from the tournament

3. **Re-populate with Approved Players**
   - Query all players with status = APPROVED
   - Create new AuctionPlayer entries for each
   - Set sort order starting from 1 in order of registration
   - **Default base price from CURRENT tournament settings** (picks up any price changes)

4. **Reset Team Finances Using Current Tournament Settings**
   - For each team in the tournament:
     - Delete old team purse record
     - Recalculate with current tournament values:
       - `initialPurse` = current `tournament.purseAmount` (uses updated value)
       - `currentPurse` = `initialPurse`
       - `purseUsed` = 0
       - `playersBought` = 0
       - `remainingSlots` = current `tournament.playersPerTeam` (uses updated value)
       - `reservedFund` = (remainingSlots - 1) × current `tournament.basePrice` (uses updated value)
       - `maxBidPerPlayer` = currentPurse - reservedFund
       - `availableForBidding` = maxBidPerPlayer

---

## Key Implementation Details

### Transactional Safety
- Both endpoints are marked as `@Transactional`
- All database changes are atomic - either fully succeed or fully rollback
- No partial updates left behind on errors

### Team Purse Recalculation
- Uses existing `TeamPurseService` methods:
  - `updatePurseOnPlayerUnsold()` - Refund a sold player
  - `initializePurse()` - Reset purse to initial state

### Validation
- Ownership verification - Only tournament owner can reset
- Player exists check - Skips non-existent or wrong tournament players
- Transitive error handling - Cascade deletes handled properly

---

## Use Cases

### Reset Specific Players
- Player registration status changed from REJECTED to APPROVED
- Need to add a specific player back to auction after removal
- Fix accidental player deletions from auction pool
- Revert an unsold player back to available

### Reset Entire Auction
- Start a complete re-auction after first auction concluded
- Correct systematic setup errors in the initial auction
- Test auction flow with fresh data
- Multiple auction rounds for same tournament

---

## Notes

- Approved players who are already UPCOMING will remain UPCOMING
- Non-approved or removed players will be marked UNSOLD
- This operation preserves all registered player data
- Only auction pool and team financial data is affected
- All changes are immediately reflected in auction and purse APIs




