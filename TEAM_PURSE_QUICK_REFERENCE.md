# Team Purse Logic - Quick Reference

## Core Concept
🎯 **Tournament Purse = Individual Team Purse (NOT divided by total teams)**

## Formula Breakdown

### Initial Setup per Team
```
Given:
- tournament.purseAmount = 100,000 (per team)
- playersPerTeam = 11

Calculations:
maxBidPerPlayer = ROUND(100,000 × 0.25) = 25,000
costPerPlayer = 100,000 ÷ 11 = 9,091
minPlayersToReserve = MAX(1, 11 ÷ 3) = 3
reservedFund = 9,091 × 3 = 27,273
availableForBidding = 100,000 - 27,273 = 72,727
```

### After Each Purchase
```
Example: Buy player for 20,000

purseUsed = 0 + 20,000 = 20,000
currentPurse = 100,000 - 20,000 = 80,000
availableForBidding = 80,000 - 27,273 = 52,727
playersBought = 1
remainingSlots = 10
```

## Bid Validation Rules

```
IF soldPrice <= maxBidPerPlayer    ✅ PASS (25,000 limit)
AND soldPrice <= availableForBidding ✅ PASS (reserve protected)
AND soldPrice >= basePrice          ✅ PASS (minimum price)
THEN BID ACCEPTED ✅

OTHERWISE BID REJECTED ❌
```

## Real-World Example

```
Tournament Config:
- Purse: 100,000
- Players per Team: 11

Team 1 Initial:
  initialPurse:       100,000
  currentPurse:       100,000
  maxBidPerPlayer:    25,000
  reservedFund:       27,273
  availableForBidding: 72,727

Purchase Sequence:
1. Buy Player A @ 20,000 ✅ (20k < 72,727)
   → availableForBidding: 52,727

2. Buy Player B @ 18,000 ✅ (18k < 52,727)
   → availableForBidding: 34,727

3. Buy Player C @ 22,000 ✅ (22k < 34,727)
   → availableForBidding: 12,727

4. Try Player D @ 15,000 ❌ (15k > 12,727)
   → REJECTED (Must preserve minimum squad)

5. Buy Player D @ 10,000 ✅ (10k < 12,727)
   → availableForBidding: 2,727

Final State:
  purseUsed:           70,000
  currentPurse:        30,000
  playersBought:       4
  remainingSlots:      7
  availableForBidding: 2,727 (can't buy anyone else now)
```

## Key Points

| Item | Value | Purpose |
|------|-------|---------|
| **initialPurse** | 100,000 | Total budget given to team |
| **currentPurse** | Decreases | Money left after purchases |
| **maxBidPerPlayer** | 25,000 | Single bid limit (diversity) |
| **reservedFund** | 27,273 | Protected for minimum squad |
| **availableForBidding** | currentPurse - reservedFund | Actual budget for next bid |

## Code Implementation

```java
// In TeamPurseService.initializePurse()
Long teamPurse = tournament.getPurseAmount(); // 100,000
Integer playersPerTeam = 11;
Integer minPlayers = Math.max(1, playersPerTeam / 3); // 3
Long maxBid = Math.round(teamPurse * 0.25); // 25,000
Long costPer = teamPurse / playersPerTeam; // 9,091
Long reserved = costPer * minPlayers; // 27,273
Long availableForBidding = teamPurse - reserved; // 72,727

// In AuctionPlayerService.sell()
var teamPurse = teamPurseService.findByTeamAndTournament(teamId, tournamentId);

if (soldPrice > teamPurse.getMaxBidPerPlayer()) {
    throw new Exception("Exceeds max bid"); // Check 1
}

if (soldPrice > teamPurse.getAvailableForBidding()) {
    throw new Exception("Insufficient available purse"); // Check 2
}

// Then update
teamPurseService.updatePurseOnPlayerSold(team, tournament, soldPrice);
```

## Summary

✅ Tournament purse is individual team allocation (100,000 each)
✅ Max bid is 25% of team purse (25,000 limit)
✅ Reserved fund ensures minimum squad (27,273)
✅ Available for bidding = current purse - reserved fund
✅ Purse reduces as players are purchased
✅ Clear validation prevents overspending

