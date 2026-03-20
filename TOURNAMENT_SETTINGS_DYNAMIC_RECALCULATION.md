# Tournament Settings Dynamic Recalculation - Technical Details

## Overview
When the "Reset Entire Auction" endpoint is called, the system automatically picks up and applies any changes made to tournament settings including:
- Base Price per player
- Number of Players Per Team
- Purse Amount per team

This ensures that if tournament configurations are updated before a re-auction, the new settings are properly reflected in all calculations.

---

## Implementation Flow

### Step 1: Fetch Current Tournament Settings
```java
Tournament tournament = tournamentService.findAndVerifyOwner(tournamentId, user);
```
The tournament is loaded with all current settings at the time of the reset.

### Step 2: Refund All Sold Players
```java
for (AuctionPlayer ap : auctionPlayers) {
    if (ap.getSoldToTeam() != null && ap.getSoldPrice() != null) {
        teamPurseService.updatePurseOnPlayerUnsold(ap.getSoldToTeam(), tournament, ap.getSoldPrice());
    }
}
```
Teams get refunded using the current tournament reference.

### Step 3: Delete Previous Auction Data
```java
auctionPlayerRepository.deleteAll(auctionPlayers);
```
Removes all old auction players from the pool.

### Step 4: Re-Add Approved Players with Current Settings
```java
for (Player player : approvedPlayers) {
    AuctionPlayer ap = AuctionPlayer.builder()
            .basePrice(tournament.getBasePrice())  // ← Uses CURRENT base price
            .tournament(tournament)
            .build();
    auctionPlayerRepository.save(ap);
}
```
Each player is re-added with the current tournament's base price setting.

### Step 5: Reset Team Purses Using Current Settings
```java
for (Team team : teams) {
    teamPurseService.initializePurse(team, tournament);
}
```
This is where all tournament settings are applied. The `initializePurse` method reads from the tournament object:

```java
@Transactional
public TeamPurse initializePurse(Team team, Tournament tournament) {
    // Reads CURRENT values from tournament
    Long teamPurse = tournament.getPurseAmount();        // Current purse amount
    Integer playersPerTeam = tournament.getPlayersPerTeam();  // Current players per team
    Long basePrice = tournament.getBasePrice();          // Current base price
    
    // Calculates based on current settings
    Long reserved = (long) (playersPerTeam - 1) * basePrice;
    Long maxBid = teamPurse - reserved;
    
    // Creates fresh purse record with current values
    TeamPurse tp = TeamPurse.builder()
            .initialPurse(teamPurse)
            .currentPurse(teamPurse)
            .purseUsed(0L)
            .maxBidPerPlayer(maxBid)
            .reservedFund(reserved)
            .availableForBidding(maxBid)
            .playersBought(0)
            .remainingSlots(playersPerTeam)
            .build();
    return teamPurseRepository.save(tp);
}
```

---

## Settings Propagation Examples

### Example 1: Base Price Changed
**Before Reset:**
- Tournament basePrice: 5000
- Team has 3 players remaining, purse 100000
- reservedFund = (3-1) × 5000 = 10000
- maxBidPerPlayer = 100000 - 10000 = 90000

**Admin Updates Base Price:** 10000

**After Reset:**
- Tournament basePrice: 10000 (NEW)
- All auction players re-added with basePrice = 10000
- For all teams: reservedFund = (3-1) × 10000 = 20000
- For all teams: maxBidPerPlayer = 100000 - 20000 = 80000

**Result:** Teams have less maxBid per player because reserve fund increased.

---

### Example 2: Purse Amount Changed
**Before Reset:**
- Tournament purseAmount: 5000000
- Team has purse 5000000, 11 remaining slots
- maxBidPerPlayer = 5000000 - (11-1)×50000 = 4500000

**Admin Updates Purse Amount:** 6000000

**After Reset:**
- Tournament purseAmount: 6000000 (NEW)
- initialPurse for all teams: 6000000
- currentPurse for all teams: 6000000 (fresh start)
- purseUsed for all teams: 0
- For all teams: maxBidPerPlayer = 6000000 - (11-1)×50000 = 5500000

**Result:** Teams have higher budget, can bid more per player.

---

### Example 3: Players Per Team Changed
**Before Reset:**
- Tournament playersPerTeam: 11
- Team has purse 5000000, remaining slots calculated as 11
- reservedFund = (11-1) × 50000 = 500000
- maxBidPerPlayer = 5000000 - 500000 = 4500000

**Admin Updates Players Per Team:** 13

**After Reset:**
- Tournament playersPerTeam: 13 (NEW)
- For all teams: remainingSlots = 13
- For all teams: reservedFund = (13-1) × 50000 = 600000
- For all teams: maxBidPerPlayer = 5000000 - 600000 = 4400000

**Result:** Teams need to reserve more for additional slots, reducing maxBid per player.

---

## Data Consistency

### Atomic Transaction
- The entire reset operation is wrapped in `@Transactional`
- If any step fails, ALL changes are rolled back
- No partial resets left behind

### Cascade Handling
- Old team purse records are deleted during `initializePurse()`
- Fresh records are created with current settings
- No stale data persists

### Validation
- Tournament ownership verified before reset
- All current values read directly from database at reset time
- No cached or stale settings used

---

## Response Confirmation

The response includes `appliedTournamentSettings` to confirm which settings were used:

```json
{
  "appliedTournamentSettings": {
    "purseAmount": 6000000,
    "playersPerTeam": 13,
    "basePrice": 50000
  }
}
```

This allows the client to verify that the correct settings were applied during the reset.

---

## Important Notes

1. **No Player Data Loss**
   - All registered players remain in the system
   - Only auction pool is refreshed
   - Player approvals/rejections are unchanged

2. **Fresh Financial State**
   - All team purses reset to initial values
   - No carried-over balances from previous auction
   - Clean slate for new auction with new settings

3. **Immediate Effect**
   - Changes are visible immediately after reset
   - No caching or delayed updates
   - Ready for auction to resume with new settings

4. **Reversibility**
   - If settings are wrong, can be corrected and reset again
   - Each reset is independent
   - Previous auction data is archived (deleted)

---

## Testing the Feature

### Test Scenario 1: Verify Base Price Applied
```bash
# 1. Create tournament with basePrice = 50000
# 2. Update to basePrice = 100000
# 3. Reset entire auction
# 4. Get auction players
#    → All should have basePrice = 100000
```

### Test Scenario 2: Verify Purse Recalculation
```bash
# 1. Create tournament with purseAmount = 5000000
# 2. Update to purseAmount = 10000000
# 3. Reset entire auction
# 4. Get team purse for any team
#    → initialPurse should be 10000000
#    → currentPurse should be 10000000
#    → maxBidPerPlayer should be recalculated
```

### Test Scenario 3: Verify Players Per Team Applied
```bash
# 1. Create tournament with playersPerTeam = 11
# 2. Update to playersPerTeam = 15
# 3. Reset entire auction
# 4. Get team purse for any team
#    → remainingSlots should be 15
#    → reservedFund should reflect new slots
```

---

## Performance Considerations

- **Database Hits:** O(A + P + T) where A=auction players, P=players, T=teams
- **Time Complexity:** O(T) dominant factor (team purse reset)
- **Recommended:** Run during off-peak hours for large tournaments (1000+ players)

---

## Future Enhancements

1. **Partial Settings Reset** - Reset only specific settings
2. **Settings History** - Track changes to tournament settings
3. **Dry-Run Mode** - Preview what would change without committing
4. **Batch Operations** - Reset multiple tournaments at once

