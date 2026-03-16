# Team Purse Logic - UPDATED (Corrected Version)

## Problem Fixed
❌ OLD: Tournament purse was being divided by total teams (100,000 / 5 = 20,000 per team)
✅ NEW: Tournament purse IS the individual team purse (100,000 per team directly)

## Updated Logic

### 1. Initial Purse Setup
```
EACH team gets: tournament.purseAmount (NOT divided by number of teams)

Example:
Tournament Purse: 100,000 (This is per team, not total)
Each team gets: 100,000
```

### 2. Per-Team Budget Breakdown
```
Team's Initial Purse: 100,000
├─ Max Bid Per Player: 25,000 (25% of 100,000)
│  └─ Prevents single huge purchase (max 25,000 per player)
│
└─ Reserved Fund: Calculated for minimum squad protection
   ├─ Min Players to Reserve: 3 (11 ÷ 3 for 11-player squad)
   ├─ Cost per Player: 9,091 (100,000 ÷ 11)
   └─ Reserved = 9,091 × 3 = 27,273
      └─ Ensures team can buy minimum squad

Available for Bidding (Initially): 72,727 (100,000 - 27,273)
```

### 3. Purse Deduction on Player Purchase
```
When player is bought:
- purseUsed += soldPrice
- currentPurse = initialPurse - purseUsed
- availableForBidding = currentPurse - reservedFund

Example:
After buying Player A @ 15,000:
- currentPurse: 85,000 (100,000 - 15,000)
- availableForBidding: 57,727 (85,000 - 27,273)
```

### 4. Bid Validation (Enforced Rules)
When selling/buying a player, validation checks:
```
✅ Rule 1: soldPrice ≤ maxBidPerPlayer (25,000 limit)
✅ Rule 2: soldPrice ≤ availableForBidding (respects reserved fund)
✅ Rule 3: soldPrice ≥ basePrice (minimum price check)
```

## Validation Examples

### Example 1: Initial State
```
Tournament Purse: 100,000 per team
Players Per Team: 11

Team 1 Budget:
├─ initialPurse: 100,000
├─ currentPurse: 100,000
├─ maxBidPerPlayer: 25,000 (25% limit)
├─ reservedFund: 27,273 (for minimum squad)
└─ availableForBidding: 72,727 (100,000 - 27,273)
```

### Example 2: After Purchasing 2 Players
```
Purchases:
- Player A @ 20,000 ✅
- Player B @ 15,000 ✅

Team 1 Budget After:
├─ initialPurse: 100,000
├─ currentPurse: 65,000 (100,000 - 20,000 - 15,000)
├─ purseUsed: 35,000
├─ maxBidPerPlayer: 25,000 (unchanged)
├─ reservedFund: 27,273 (unchanged)
├─ availableForBidding: 37,727 (65,000 - 27,273)
├─ playersBought: 2
└─ remainingSlots: 9
```

### Example 3: Bid Rejection Scenario
```
Current State:
- currentPurse: 32,000
- maxBidPerPlayer: 25,000
- reservedFund: 27,273
- availableForBidding: 4,727 (32,000 - 27,273)

Try to buy Player C @ 10,000
❌ REJECTED

Reason: 10,000 > 4,727 (availableForBidding)
Although currentPurse (32,000) is enough, must reserve 27,273
for minimum squad protection
```

## Files Updated

### `/home/hari/proj/auction/src/main/java/com/bid/auction/service/TeamPurseService.java`

**Changes:**
1. ✅ `initializePurse()` - Now uses `tournament.getPurseAmount()` directly (not divided)
2. ✅ `recalculateAllTeamPurses()` - Updated to use purse amount directly
3. ✅ Removed `calculatePursePerTeam()` - No longer needed

**Key Code:**
```java
// Tournament purse IS the individual team purse (not divided)
Long teamPurse = tournament.getPurseAmount() != null && tournament.getPurseAmount() > 0 
        ? tournament.getPurseAmount() : 1000000L;
```

### `/home/hari/proj/auction/src/main/java/com/bid/auction/service/AuctionPlayerService.java`

**Already Updated Previous:** 
- Uses `TeamPurse` entity for validation
- Validates against `maxBidPerPlayer` (25% limit)
- Validates against `availableForBidding` (respects reserved fund)

## Key Differences: OLD vs NEW

| Aspect | OLD ❌ | NEW ✅ |
|--------|--------|--------|
| **Initial Purse** | 100,000 ÷ 5 = 20,000 per team | 100,000 per team (direct) |
| **Max Bid** | 5,000 (25% of 20k) | 25,000 (25% of 100k) |
| **Reserved Fund** | 5,454 | 27,273 |
| **Available for Bid** | 14,546 | 72,727 |
| **Total per Team** | 20,000 | 100,000 |

## Benefits

✅ **Fair Allocation**: Each team gets the full purse amount, not divided
✅ **Realistic Budgets**: Teams have reasonable budgets for player purchases
✅ **Reserve Protection**: Still maintains reserve fund for minimum squad
✅ **Diversity**: Max bid per player (25%) prevents hoarding
✅ **Transparent**: Clear and understandable budget limits

## Migration Notes

If you have existing tournaments/teams in the database:
- Run `recalculateAllTeamPurses()` after updating the tournament purse amount
- Or delete and reinitialize team purses (will trigger re-creation)

Example:
```java
Tournament tournament = tournamentService.findById(tournamentId);
teamPurseService.recalculateAllTeamPurses(tournament);
```

