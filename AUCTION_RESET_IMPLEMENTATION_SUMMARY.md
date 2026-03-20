# Auction Reset API - Implementation Summary

## Changes Made

### 1. New Request DTO
**File**: `src/main/java/com/bid/auction/dto/request/ResetAuctionPlayersRequest.java`

Request body for resetting specific players:
```java
{
  "playerIds": [1, 2, 3, ...]  // List of auction player IDs to reset
}
```

### 2. AuctionPlayerService Enhancements
**File**: `src/main/java/com/bid/auction/service/AuctionPlayerService.java`

Added two new transactional methods:

#### a) `resetAuctionPlayers(tournamentId, playerIds, user)`
- Resets specific auction players
- Refunds sold players back to teams
- Sets APPROVED players to UPCOMING (available)
- Sets non-APPROVED players to UNSOLD (unavailable)
- Recalculates all affected team purses

Returns:
```json
{
  "processedCount": int,
  "skippedCount": int,
  "totalRequested": int
}
```

#### b) `resetEntireAuction(tournamentId, user)`
- Completely resets auction for a tournament
- Refunds all sold players to teams
- Deletes all existing auction entries
- Re-creates auction pool with all APPROVED players
- Resets purses for all teams

Returns:
```json
{
  "deletedAuctionPlayers": int,
  "readdedApprovedPlayers": int,
  "teamsReset": int
}
```

### 3. AuctionPlayerController Additions
**File**: `src/main/java/com/bid/auction/controller/AuctionPlayerController.java`

Added two new REST endpoints:

#### Endpoint 1: Reset Specific Players
```
POST /tournaments/{tournamentId}/auction-players/reset
Content-Type: application/json

{
  "playerIds": [10, 11, 12]
}
```

#### Endpoint 2: Reset Entire Auction
```
POST /tournaments/{tournamentId}/auction/reset-entire
```

---

## Technical Details

### Database Transactions
- Both operations use `@Transactional` annotation
- Atomic operations - all changes succeed or all rollback
- Proper cascade handling for related entities

### Team Purse Logic
For each affected team:

**When refunding a sold player:**
- Deduct sold price from `purseUsed`
- Recalculate `currentPurse` = `initialPurse` - `purseUsed`
- Update `playersBought` (decrement)
- Update `remainingSlots` (increment)
- Recalculate `reservedFund` = (remainingSlots - 1) × basePrice
- Recalculate `maxBidPerPlayer` = currentPurse - reservedFund

**When resetting entire auction:**
- Reset all values to initial state
- Use `TeamPurseService.initializePurse()` for each team

### Validation
- ✅ Tournament ownership verification
- ✅ Player existence check
- ✅ Tournament ID validation for each player
- ✅ Error handling and cascading deletes
- ✅ Proper HTTP status codes

---

## API Endpoints Summary

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/tournaments/{id}/auction-players/reset` | Reset specific players |
| POST | `/tournaments/{id}/auction/reset-entire` | Reset entire auction |

---

## Integration Points

### Existing Services Used
1. **TournamentService**
   - `findAndVerifyOwner()` - Verify tournament ownership

2. **TeamPurseService**
   - `updatePurseOnPlayerUnsold()` - Refund sold players
   - `initializePurse()` - Reset team purses

3. **AuctionPlayerRepository**
   - `findById()`, `findByTournamentId()`, `deleteAll()`

4. **PlayerRepository**
   - `findByTournamentAndStatus()` - Get approved players

5. **TeamRepository**
   - `findByTournamentId()` - Get teams for reset

### No External Dependencies
- Uses existing Spring Data JPA repositories
- No new external libraries required
- Follows existing code patterns and structure

---

## Testing Recommendations

### Test Case 1: Reset Specific Players
1. Create tournament with teams and auction players
2. Sell some players to teams
3. Call reset endpoint with specific player IDs
4. Verify:
   - Players marked as UPCOMING/UNSOLD based on status
   - Team purses refunded correctly
   - Sold assignments cleared
   - Team financial metrics recalculated

### Test Case 2: Reset Entire Auction
1. Create tournament with approved and non-approved players
2. Auction some players to teams
3. Call reset entire auction endpoint
4. Verify:
   - All old auction players deleted
   - Only approved players re-added
   - All teams have fresh purses
   - Sort order maintained
   - All sold prices refunded

### Test Case 3: Error Handling
1. Call with invalid tournament ID → 404
2. Call as non-owner user → 403
3. Call with non-existent player ID → Skipped in specific reset
4. Call with empty player list → Returns 0 processed

---

## Files Modified/Created

| File | Type | Changes |
|------|------|---------|
| `AuctionPlayerService.java` | Modified | Added 2 methods (~80 lines) |
| `AuctionPlayerController.java` | Modified | Added 2 endpoints + import |
| `ResetAuctionPlayersRequest.java` | Created | New request DTO |
| `AUCTION_RESET_API_DOCUMENTATION.md` | Created | API documentation |

---

## Compilation Status
✅ **No errors** - Project compiles successfully
⚠️ **Warnings** - Only unused method warnings (expected for new features)

---

## Next Steps (Optional)

1. **Add unit tests** for the new service methods
2. **Add integration tests** for the new endpoints
3. **Add API documentation** to Swagger/OpenAPI if configured
4. **Monitor performance** for large tournaments with many players
5. **Consider rate limiting** if reset is resource-intensive

---

## Notes

- All player registration data is preserved (no players deleted)
- Only auction pool and financial data are affected
- Multiple resets can be performed without data loss
- Transaction safety ensures no partial updates
- Follows Spring Boot best practices

