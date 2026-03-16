# Max Bid Per Player Logic - Updated

## What Changed

**OLD Logic:**
```
maxBidPerPlayer = ROUND(teamPurse × 0.25)
Example: 100,000 × 0.25 = 25,000
```

**NEW Logic:**
```
maxBidPerPlayer = teamPurse - reservedFund
Example: 100,000 - 27,273 = 72,727
```

## Rationale

✅ Each team can bid UP TO their available purse (after ensuring minimum squad can be completed)
✅ No arbitrary 25% limit - teams get full flexibility with their available budget
✅ maxBidPerPlayer = availableForBidding (same value)
✅ Clear: Maximum bid is limited by what's available after reserving minimum squad

## Formula Breakdown

### Initial Purse Setup per Team
```
Given:
- tournament.purseAmount = 100,000 (individual team purse)
- playersPerTeam = 11

Calculations:
costPerPlayer = 100,000 ÷ 11 = 9,091
minPlayersToReserve = MAX(1, 11 ÷ 3) = 3
reservedFund = 9,091 × 3 = 27,273

maxBidPerPlayer = 100,000 - 27,273 = 72,727 ✅ (NOT 25,000)
availableForBidding = 72,727 (same as maxBidPerPlayer)
```

## Validation Rules (Updated)

```
When selling a player:

✅ Rule 1: soldPrice ≤ maxBidPerPlayer (72,727 limit)
✅ Rule 2: soldPrice ≤ availableForBidding (which updates as purchases are made)
✅ Rule 3: soldPrice ≥ basePrice (minimum price check)

THEN BID ACCEPTED ✅
OTHERWISE BID REJECTED ❌
```

## Real-World Examples

### Example 1: Initial State
```
Tournament Config:
- Purse: 100,000 per team
- Players per Team: 11

Team 1 Initial State:
  initialPurse:       100,000
  currentPurse:       100,000
  maxBidPerPlayer:    72,727 (100,000 - 27,273) ✅
  reservedFund:       27,273 (for minimum 3 players)
  availableForBidding: 72,727 (same as maxBidPerPlayer)
```

### Example 2: First Purchase
```
Try to buy Player A @ 60,000

Validation:
✅ 60,000 ≤ 72,727 (maxBidPerPlayer) - PASS
✅ 60,000 ≤ 72,727 (availableForBidding) - PASS

After purchase:
  currentPurse:       40,000 (100,000 - 60,000)
  availableForBidding: 12,727 (40,000 - 27,273)
  maxBidPerPlayer:    72,727 (unchanged - based on initial purse)
```

### Example 3: Next Attempts
```
Try to buy Player B @ 20,000

Validation:
✅ 20,000 ≤ 72,727 (maxBidPerPlayer) - PASS
✅ 20,000 ≤ 12,727 (availableForBidding) - FAIL ❌
REJECTED

Reason: Only 12,727 available after reserving 27,273 for minimum squad.
Must protect minimum squad fund even though maxBidPerPlayer is 72,727.
```

### Example 4: Conservative Bid
```
Try to buy Player B @ 10,000

Validation:
✅ 10,000 ≤ 72,727 (maxBidPerPlayer) - PASS
✅ 10,000 ≤ 12,727 (availableForBidding) - PASS

After purchase:
  currentPurse:       30,000
  availableForBidding: 2,727 (30,000 - 27,273)
  playersBought:      2
```

## Key Differences: Before vs After

| Metric | Before ❌ | After ✅ | Notes |
|--------|----------|---------|-------|
| **initialPurse** | 100,000 | 100,000 | Unchanged |
| **reservedFund** | 27,273 | 27,273 | Unchanged |
| **maxBidPerPlayer** | 25,000 (25%) | 72,727 (100% - reserved) | **CHANGED** |
| **availableForBidding (initial)** | 72,727 | 72,727 | Same now |
| **Flexibility** | Limited (25% cap) | Full available purse | **More flexible** |

## Impact on Teams

✅ **More flexibility**: Teams can bid larger amounts (up to 72,727 instead of 25,000)
✅ **Fair allocation**: Bid limit = available purse (what's actually spendable)
✅ **Protected minimum squad**: Still maintains reservation for 3 minimum players
✅ **Transparent logic**: maxBidPerPlayer = availableForBidding (clear relationship)

## Code Changes

### File: `/home/hari/proj/auction/src/main/java/com/bid/auction/service/TeamPurseService.java`

**In `initializePurse()` method:**
```java
// OLD
Long maxBid = Math.round(teamPurse * 0.25);

// NEW
Long maxBid = teamPurse - reserved;
```

**In `recalculateAllTeamPurses()` method:**
```java
// OLD
Long maxBid = Math.round(teamPurse * 0.25);

// NEW
Long maxBid = teamPurse - reserved;
```

## Update Scenario with Numbers

```
Tournament: 100,000 per team, 11 players

OLD System (25% rule):
- Max single bid: 25,000
- Min for 3 players: 27,273
- Available after reserve: 72,727
- Problem: Can bid 25,000 on one player but have 72,727 available?

NEW System (100% - reserved):
- Max single bid: 72,727 (actual available)
- Min for 3 players: 27,273
- Available after reserve: 72,727
- Solution: Max bid = actual available ✅
```

## Summary

✅ `maxBidPerPlayer` now equals `availableForBidding`
✅ Teams can bid up to their full available purse (not 25% arbitrary limit)
✅ Reserved fund still protects minimum squad
✅ More flexible and fair bidding system
✅ Clearer logic: bid limit = purse available

## Migration Notes

For existing tournaments:
```java
Tournament tournament = tournamentService.findById(tournamentId);
teamPurseService.recalculateAllTeamPurses(tournament);
```

This will update all team purses with new maxBidPerPlayer calculations.

