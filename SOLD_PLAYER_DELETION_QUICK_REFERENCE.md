# SOLD Player Deletion - Quick Reference

## What Happens When a SOLD Player is Deleted?

When you delete a player with status **SOLD**, the system automatically:

### 1. Refunds the Team
- **Sold Price**: Added back to `currentPurse`
- **Purse Used**: Reduced by the sold price

### 2. Updates Player Slots
- **Players Bought**: Decremented by 1
- **Remaining Slots**: Incremented by 1

### 3. Recalculates Team Constraints
- **Reserved Fund**: `(remainingSlots - 1) × basePrice`
- **Max Bid Per Player**: `currentPurse - reservedFund`
- **Available for Bidding**: `currentPurse - reservedFund`

## Example

**Before Deletion:**
```
Tournament Settings:
  - Base Price: ₹5,000
  - Players Per Team: 11

Team Status:
  - Initial Purse: ₹1,000,000
  - Current Purse: ₹750,000
  - Purse Used: ₹250,000
  - Players Bought: 5
  - Remaining Slots: 6
  - Reserved Fund: ₹25,000 (5 × ₹5,000)
  - Max Bid: ₹725,000
  - Available for Bidding: ₹725,000
```

**Player Deleted (Sold for ₹100,000):**
```
Team Status After Deletion:
  - Current Purse: ₹850,000 ✅ (increased)
  - Purse Used: ₹150,000 ✅ (decreased)
  - Players Bought: 4 ✅ (decremented)
  - Remaining Slots: 7 ✅ (incremented)
  - Reserved Fund: ₹30,000 ✅ (recalculated)
  - Max Bid: ₹820,000 ✅ (recalculated)
  - Available for Bidding: ₹820,000 ✅ (recalculated)
```

## Code Flow

```
DELETE /api/players/{id}
    ↓
PlayerService.delete(id, user)
    ↓
AuctionPlayerService.deletePlayerWithAuctionRefunds(id)
    ↓
AuctionPlayerService.removeFromAuctionIfPresent(id)
    ↓
For each SOLD AuctionPlayer:
  TeamPurseService.updatePurseOnPlayerUnsold(team, tournament, price)
    ↓
Delete auction player records
    ↓
Delete player record
```

## Key Methods

### PlayerService.delete()
- Verifies tournament ownership
- Calls `deletePlayerWithAuctionRefunds()`
- Deletes player record

### AuctionPlayerService.removeFromAuctionIfPresent()
- Gets all auction players linked to player
- Checks if `soldToTeam != null && soldPrice != null`
- Calls `TeamPurseService.updatePurseOnPlayerUnsold()`
- Deletes auction player records

### TeamPurseService.updatePurseOnPlayerUnsold()
- Refunds: `purseUsed -= unsolvedPrice`
- Updates: `currentPurse = initialPurse - purseUsed`
- Updates: `playersBought--`
- Updates: `remainingSlots++`
- Recalculates: `reservedFund = (remainingSlots - 1) × basePrice`
- Recalculates: `availableForBidding = currentPurse - reservedFund`
- Recalculates: `maxBidPerPlayer = currentPurse - reservedFund`

## Transaction Safety

All operations are atomic:
- Either all updates succeed
- Or all rollback together
- No partial updates

## Related Features

- **Mark Player Unsold**: Similar logic, but player remains in auction
- **Player Rejection**: Also calls `removeFromAuctionIfPresent()`
- **Auction Reset**: Refunds all sold players to teams

## Testing

To test this feature:

1. Create a tournament with purse settings
2. Create teams and initialize purses
3. Add players to auction
4. Sell a player to a team
5. Verify team purse decreases
6. Delete the sold player
7. Verify team purse increases back
8. Verify all values recalculated correctly

## API Response

**Request:**
```bash
DELETE /api/players/123
```

**Response:**
```
204 No Content
```

No response body, but database updated with:
- Team purse refunded
- All values recalculated
- Player deleted
- Auction player deleted

## Error Handling

If any of these conditions fail, the entire operation rolls back:
- Tournament owner verification fails
- Team purse not found
- Database write fails

Status: `500 Internal Server Error` or `403 Forbidden`

