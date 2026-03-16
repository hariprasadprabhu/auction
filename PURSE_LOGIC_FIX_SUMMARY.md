# Team Purse Logic Fix - Summary

## Problem Statement
The initial team purse and bidding validation had the following issues:
1. ❌ Initial purse was being calculated per team, but validation was checking against **total tournament purse** instead of **team's allocated purse**
2. ❌ No validation for **max bid per player** (25% of team's purse)
3. ❌ Reserved fund logic was not being enforced during bidding

## Solution
Updated the purse validation logic in `AuctionPlayerService.sell()` method to use the `TeamPurse` entity which has the correct calculations.

### Changes Made

#### File: `/home/hari/proj/auction/src/main/java/com/bid/auction/service/AuctionPlayerService.java`

**Before:**
```java
// Old logic - WRONG
List<AuctionPlayer> soldPlayers = auctionPlayerRepository.findBySoldToTeamId(team.getId());
long purseUsed = soldPlayers.stream()
        .mapToLong(p -> p.getSoldPrice() != null ? p.getSoldPrice() : 0L).sum();
long purseRemaining = tournament.getPurseAmount() - purseUsed;  // ❌ Uses TOTAL tournament purse!
if (purseRemaining < req.getSoldPrice()) {
    throw new IllegalArgumentException(
            "Team's remaining purse (" + purseRemaining + ") is less than the sold price");
}
```

**After:**
```java
// New logic - CORRECT
var teamPurse = teamPurseService.findByTeamAndTournament(team.getId(), tournament.getId());

// Validate sold price does not exceed max bid per player (25% of team's purse)
if (req.getSoldPrice() > teamPurse.getMaxBidPerPlayer()) {
    throw new IllegalArgumentException(
            "Sold price (" + req.getSoldPrice() + ") exceeds max bid per player (" + 
            teamPurse.getMaxBidPerPlayer() + ")");
}

// Validate team's available purse (currentPurse - reservedFund)
if (teamPurse.getAvailableForBidding() < req.getSoldPrice()) {
    throw new IllegalArgumentException(
            "Team's available purse for bidding (" + teamPurse.getAvailableForBidding() + 
            ") is less than the sold price (" + req.getSoldPrice() + ")");
}
```

## Logic Flow

### 1. **Initial Purse Setup** (Already Correct in `TeamPurseService.initializePurse()`)
```
initialPurse = tournament.purseAmount / tournament.totalTeams
Example: 100,000 / 5 teams = 20,000 per team ✅
```

### 2. **Purse Deduction on Player Purchase** (Already Correct in `TeamPurseService.updatePurseOnPlayerSold()`)
```
purseUsed += soldPrice
currentPurse = initialPurse - purseUsed
Example: 20,000 - 5,000 (bought player) = 15,000 remaining ✅
```

### 3. **Reserved Fund Calculation** (Already Correct)
```
minPlayersToReserveFor = max(1, playersPerTeam / 3)
costPerPlayer = initialPurse / playersPerTeam
reservedFund = costPerPlayer × minPlayersToReserveFor
availableForBidding = currentPurse - reservedFund

Example with 11 players per team:
- costPerPlayer = 20,000 / 11 = 1,818
- minPlayersToReserveFor = 11 / 3 = 3 (rounded)
- reservedFund = 1,818 × 3 = 5,454
- availableForBidding = currentPurse - 5,454
```

### 4. **Bid Validation** (NOW FIXED)
When selling a player, the system now validates:
✅ Bid amount ≤ maxBidPerPlayer (25% of team's purse)
✅ Bid amount ≤ availableForBidding (currentPurse - reservedFund)
✅ Bid amount ≥ basePrice (existing check, maintained)

## Validation Examples

### Example Scenario: Tournament with 100,000 purse, 5 teams, 11 players per team

**Team's Allocation:**
- initialPurse: 20,000
- maxBidPerPlayer: 5,000 (25% of 20,000)
- costPerPlayer: 1,818 (20,000 / 11)
- reservedFund: 5,454 (1,818 × 3 players minimum)
- availableForBidding (initially): 14,546 (20,000 - 5,454)

**Purchase Scenario:**
1. Buy Player 1 @ 5,000
   - ✅ Pass: 5,000 ≤ 5,000 (maxBid)
   - ✅ Pass: 5,000 ≤ 14,546 (availableForBidding)
   - After: currentPurse = 15,000, availableForBidding = 9,546

2. Buy Player 2 @ 5,000
   - ✅ Pass: 5,000 ≤ 5,000 (maxBid)
   - ✅ Pass: 5,000 ≤ 9,546 (availableForBidding)
   - After: currentPurse = 10,000, availableForBidding = 4,546

3. Try to buy Player 3 @ 5,000
   - ❌ FAIL: 5,000 > 4,546 (availableForBidding)
   - Reason: Reservation for minimum squad prevents spending below 5,454

## Benefits of This Fix

1. **Fairness**: Each team gets an equal share of the tournament purse
2. **Reserve Protection**: Ensures teams can complete their minimum squad (≈33% of required players)
3. **Diversity**: Max bid per player prevents hoarding (25% limit)
4. **Accuracy**: Uses the dedicated `TeamPurse` entity for all calculations instead of recalculating
5. **Consistency**: All validations align with the database schema and business logic

## Files Modified

- `/home/hari/proj/auction/src/main/java/com/bid/auction/service/AuctionPlayerService.java` - Sell method validation logic

## Files NOT Modified (Already Correct)

- `TeamPurseService.java` - Purse initialization and update logic
- `TeamPurse.java` - Entity definition
- All other related files

## Testing Recommendations

Test the following scenarios:
1. ✅ Verify maxBidPerPlayer is enforced (25% rule)
2. ✅ Verify availableForBidding respects reserved fund
3. ✅ Verify purse deductions are calculated correctly
4. ✅ Verify teams can complete minimum squad
5. ✅ Verify fairness - each team gets equal initial purse

