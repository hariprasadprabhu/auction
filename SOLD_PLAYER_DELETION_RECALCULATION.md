# SOLD Player Deletion - Team Purse Recalculation

## Overview

When a player with **SOLD** status is deleted from the system, the feature automatically refunds the sold amount back to the respective team and recalculates all team financial values.

## Feature Details

### When Player is Deleted with Status = SOLD

The system performs the following operations:

1. **Refund Team Purse**
   - Adds the sold price back to the team's `currentPurse`
   - Deducts the sold price from the team's `purseUsed`

2. **Recalculate Player Slots**
   - Decrements `playersBought` by 1
   - Increments `remainingSlots` by 1

3. **Recalculate Reserved Fund**
   - Formula: `(remainingSlots - 1) × basePrice`
   - Adjusts minimum squad fund based on new remaining slots

4. **Recalculate Max Bid Per Player**
   - Formula: `currentPurse - reservedFund`
   - Maximum amount team can bid for a single player

5. **Recalculate Available Purse for Bidding**
   - Formula: `currentPurse - reservedFund`
   - Amount available after ensuring minimum squad fund

## Code Implementation

### 1. PlayerService.delete() Method

**Location:** `src/main/java/com/bid/auction/service/PlayerService.java`

```java
@Transactional
public void delete(Long id, User user) {
    Player player = findPlayer(id);
    Long tournamentId = player.getTournament().getId();
    tournamentService.findAndVerifyOwner(tournamentId, user);
    
    // When player status is SOLD:
    // - Refund the team
    // - Recalculate team values
    // - Delete auction player records
    auctionPlayerService.deletePlayerWithAuctionRefunds(id);
    
    // Delete the player
    playerRepository.delete(player);
}
```

### 2. AuctionPlayerService.removeFromAuctionIfPresent() Method

**Location:** `src/main/java/com/bid/auction/service/AuctionPlayerService.java`

This method is called when a player is deleted or rejected:

```java
@Transactional
public void removeFromAuctionIfPresent(Long playerId) {
    // Get all auction players linked to this player
    List<AuctionPlayer> linkedAuctionPlayers = auctionPlayerRepository.findByPlayerId(playerId);
    
    // For each linked auction player, if sold to a team, refund
    for (AuctionPlayer ap : linkedAuctionPlayers) {
        if (ap.getSoldToTeam() != null && ap.getSoldPrice() != null) {
            // Call TeamPurseService to refund and recalculate
            teamPurseService.updatePurseOnPlayerUnsold(
                ap.getSoldToTeam(), 
                ap.getTournament(), 
                ap.getSoldPrice()
            );
        }
    }
    
    // Delete all auction players linked to this player
    auctionPlayerRepository.deleteByPlayerId(playerId);
}
```

### 3. TeamPurseService.updatePurseOnPlayerUnsold() Method

**Location:** `src/main/java/com/bid/auction/service/TeamPurseService.java`

This method handles all recalculations:

```java
@Transactional
public TeamPurse updatePurseOnPlayerUnsold(Team team, Tournament tournament, Long unsolvedPrice) {
    TeamPurse tp = findByTeamAndTournament(team.getId(), tournament.getId());
    
    // 1. Refund the purse
    tp.setPurseUsed(Math.max(0L, tp.getPurseUsed() - unsolvedPrice));
    tp.setCurrentPurse(tp.getInitialPurse() - tp.getPurseUsed());
    
    // 2. Update player counts
    tp.setPlayersBought(Math.max(0, tp.getPlayersBought() - 1));
    tp.setRemainingSlots(tp.getRemainingSlots() + 1);
    
    // 3. Recalculate reserved fund
    Long basePrice = tournament.getBasePrice() != null ? tournament.getBasePrice() : 5000L;
    Long reserved = (long) (tp.getRemainingSlots() - 1) * basePrice;
    tp.setReservedFund(reserved);
    
    // 4. Recalculate available for bidding
    tp.setAvailableForBidding(Math.max(0L, tp.getCurrentPurse() - reserved));
    
    // 5. Recalculate max bid per player
    Long maxBid = Math.max(0L, tp.getCurrentPurse() - reserved);
    tp.setMaxBidPerPlayer(maxBid);
    
    return teamPurseRepository.save(tp);
}
```

## Data Model - TeamPurse Fields

| Field | Description | Updates |
|-------|-------------|---------|
| `currentPurse` | Remaining budget after spending | Increased by sold price |
| `purseUsed` | Total spent on players | Decreased by sold price |
| `playersBought` | Number of players purchased | Decremented by 1 |
| `remainingSlots` | Slots remaining to fill | Incremented by 1 |
| `reservedFund` | Fund reserved for minimum squad | Recalculated: `(remainingSlots - 1) × basePrice` |
| `maxBidPerPlayer` | Max bid for single player | Recalculated: `currentPurse - reservedFund` |
| `availableForBidding` | Available after reservation | Recalculated: `currentPurse - reservedFund` |

## Example Scenario

### Initial State
- Tournament: Base Price = ₹5,000, Players Per Team = 11
- Team Purse: Initial = ₹1,000,000
- Players Bought: 5, Remaining Slots: 6
- Current Purse: ₹750,000, Purse Used: ₹250,000
- Reserved Fund: `(6 - 1) × 5,000 = ₹25,000`
- Max Bid: ₹750,000 - ₹25,000 = ₹725,000
- Available for Bidding: ₹725,000

### Player Sold for ₹100,000

After Deletion:
- Purse Used: ₹250,000 - ₹100,000 = ₹150,000
- Current Purse: ₹1,000,000 - ₹150,000 = ₹850,000
- Players Bought: 5 - 1 = 4
- Remaining Slots: 6 + 1 = 7
- Reserved Fund: `(7 - 1) × 5,000 = ₹30,000`
- Max Bid: ₹850,000 - ₹30,000 = ₹820,000
- Available for Bidding: ₹820,000

## Transaction Safety

All operations are wrapped in `@Transactional` annotations ensuring:
- **Atomicity**: All updates succeed or all rollback
- **Consistency**: Team and purse data remain consistent
- **Isolation**: No partial updates visible to other threads
- **Durability**: Changes persist to database

## Related Endpoints

### Delete Player
```
DELETE /api/players/{id}
Authorization: Required (Bearer Token)
```

Returns: **204 No Content**

Triggers chain:
1. `PlayerService.delete(id, user)`
2. `AuctionPlayerService.deletePlayerWithAuctionRefunds(id)`
3. `AuctionPlayerService.removeFromAuctionIfPresent(id)`
4. `TeamPurseService.updatePurseOnPlayerUnsold(team, tournament, price)`

## Files Modified

1. **PlayerService.java**
   - Enhanced `delete()` method with detailed comments

2. **AuctionPlayerService.java**
   - Enhanced `removeFromAuctionIfPresent()` method with comprehensive documentation
   - Added `deletePlayerWithAuctionRefunds()` as alias for backwards compatibility

3. **TeamPurseService.java**
   - `updatePurseOnPlayerUnsold()` handles all recalculations

## Testing Checklist

- [ ] Create team with initial purse and configuration
- [ ] Add players and sell them to team
- [ ] Verify team purse values after sale
- [ ] Delete one SOLD player
- [ ] Verify purse is refunded correctly
- [ ] Verify playersBought is decremented
- [ ] Verify remainingSlots is incremented
- [ ] Verify maxBidPerPlayer is recalculated
- [ ] Verify reservedFund is recalculated
- [ ] Verify availableForBidding is recalculated
- [ ] Verify transaction rolls back on error

## API Example

### Request
```bash
curl -X DELETE /api/players/123 \
  -H "Authorization: Bearer {token}"
```

### Response
```
204 No Content
```

### Behind the Scenes
If player 123 was SOLD to Team A for ₹100,000:

**Team A Purse Before:**
```json
{
  "currentPurse": 750000,
  "purseUsed": 250000,
  "playersBought": 5,
  "remainingSlots": 6,
  "reservedFund": 25000,
  "maxBidPerPlayer": 725000,
  "availableForBidding": 725000
}
```

**Team A Purse After:**
```json
{
  "currentPurse": 850000,
  "purseUsed": 150000,
  "playersBought": 4,
  "remainingSlots": 7,
  "reservedFund": 30000,
  "maxBidPerPlayer": 820000,
  "availableForBidding": 820000
}
```

## Notes

- **Player Status Dependency**: The deletion works when player status is SOLD (as tracked in the AuctionPlayer entity)
- **Cascade Behavior**: Deleting a player also deletes all linked auction player records
- **Idempotency**: If called multiple times, subsequent calls will be no-ops as the auction players are already deleted
- **Error Handling**: Transaction rollback if team purse not found or database write fails

---

## Summary

This feature ensures data consistency when SOLD players are deleted by:
1. ✅ Adding sold price back to team purse
2. ✅ Recalculating available purse
3. ✅ Recalculating required players count
4. ✅ Recalculating max bid
5. ✅ Recalculating reserved count
6. ✅ Maintaining transactional consistency

