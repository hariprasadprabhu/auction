# Team Purse Quick Reference - UI Integration

## What is Team Purse?

A system that tracks financial metrics for each team during tournament auction:
- **Current Purse**: Remaining money available for bidding
- **Max Bid Per Player**: Maximum amount team can bid on one player
- **Reserved Fund**: Money set aside for minimum squad requirements
- **Available for Bidding**: Actual spendable amount (currentPurse - reservedFund)

---

## API Endpoints

### 1. Get All Team Purses in Tournament
```
GET /api/tournaments/{tournamentId}/team-purses
Response: Array of TeamPurseResponse
```

**Use Case:** Display financial dashboard for all teams

**Response Example:**
```json
{
  "id": 5,
  "teamId": 12,
  "teamNumber": "T001",
  "teamName": "Royal Challengers",
  "tournamentId": 1,
  "initialPurse": 1000000,
  "currentPurse": 750000,
  "purseUsed": 250000,
  "maxBidPerPlayer": 250000,
  "reservedFund": 363636,
  "availableForBidding": 386364,
  "playersBought": 2,
  "remainingSlots": 9
}
```

---

### 2. Get Specific Team Purse
```
GET /api/tournaments/{tournamentId}/teams/{teamId}/purse
Response: TeamPurseResponse
```

**Use Case:** Show team budget during auction session

---

### 3. Get Team Purses Across Tournaments
```
GET /api/teams/{teamId}/purses
Response: Array of TeamPurseResponse
```

**Use Case:** Show team's participation budget across multiple tournaments

---

## Key Fields Explained

| Field | Meaning | Usage |
|-------|---------|-------|
| `initialPurse` | Starting budget | Show as reference/context |
| `currentPurse` | Money left = initialPurse - purseUsed | N/A (internal calculation) |
| `purseUsed` | Total spent on players | Show spending progress |
| `maxBidPerPlayer` | Maximum bid limit per player | Validate bid input (max value) |
| `reservedFund` | Fund reserved for minimum squad | Show as "reserved" info |
| `availableForBidding` | Real spendable budget | Use for bid validation (max value) |
| `playersBought` | Players already purchased | Show roster progress |
| `remainingSlots` | Players still to be purchased | Show squad completion % |

---

## UI Implementation Examples

### Example 1: Display Team Budget Card
```html
<div class="team-budget-card">
  <h3>{{ teamName }}</h3>
  
  <div class="budget-info">
    <p>Total Purse: ₹{{ initialPurse | currency }}</p>
    <p>Spent: ₹{{ purseUsed | currency }}</p>
    <p class="highlight">Available for Bidding: ₹{{ availableForBidding | currency }}</p>
  </div>
  
  <div class="squad-info">
    <p>Players Bought: {{ playersBought }} / {{ playersBought + remainingSlots }}</p>
    <progress value="{{ playersBought }}" max="{{ playersBought + remainingSlots }}"></progress>
  </div>
  
  <div class="reserved-info">
    <p>Reserved for Minimum Squad: ₹{{ reservedFund | currency }}</p>
  </div>
</div>
```

### Example 2: Bid Validation
```typescript
// Before allowing a bid
const validateBid = (bidAmount: number, maxBidPerPlayer: number, availableForBidding: number) => {
  if (bidAmount > maxBidPerPlayer) {
    return { valid: false, error: `Max bid is ₹${maxBidPerPlayer}` };
  }
  if (bidAmount > availableForBidding) {
    return { valid: false, error: `Available budget is ₹${availableForBidding}` };
  }
  return { valid: true };
};

// Usage
const result = validateBid(200000, 250000, 386364);
if (!result.valid) {
  showError(result.error);
}
```

### Example 3: Real-time Budget Update
```typescript
// After player is sold
async function onPlayerSold(teamId: number, tournamentId: number) {
  const updatedPurse = await http.get(
    `/api/tournaments/${tournamentId}/teams/${teamId}/purse`
  );
  
  // Update display
  this.availableForBidding = updatedPurse.availableForBidding;
  this.currentPurse = updatedPurse.currentPurse;
  this.playersBought = updatedPurse.playersBought;
  this.remainingSlots = updatedPurse.remainingSlots;
  
  // Show toast notification
  showNotification(`Budget updated! Available: ₹${updatedPurse.availableForBidding}`);
}
```

### Example 4: Dashboard View
```typescript
// Show all teams' financial status
async function loadTournamentBudgets(tournamentId: number) {
  const teams = await http.get(`/api/tournaments/${tournamentId}/team-purses`);
  
  // Sort by available budget (descending)
  teams.sort((a, b) => b.availableForBidding - a.availableForBidding);
  
  // Display in table
  return teams.map(team => ({
    teamNumber: team.teamNumber,
    teamName: team.teamName,
    remaining: `₹${team.availableForBidding}`,
    spent: `₹${team.purseUsed}`,
    players: `${team.playersBought}/${team.playersBought + team.remainingSlots}`,
    status: team.availableForBidding < 100000 ? 'warning' : 'ok'
  }));
}
```

---

## When to Fetch Purse Data

| Event | Action |
|-------|--------|
| Page Load (Auction) | Fetch all team purses for tournament |
| Player Sold | Fetch updated purse for selling team |
| Player Marked Unsold | Fetch updated purse for previously-sold-to team |
| Tournament Edited | Optionally refresh all team purses |
| Before Bid Placement | Validate against team's purse |

---

## Calculations You Should Know

These are calculated on backend - FYI only:

```
Per Team Purse = Tournament Total Purse / Number of Teams
Max Bid Per Player = Per Team Purse × 25%
Reserved Fund = (Per Team Purse / Players Per Team) × (Players Per Team / 3)
Available for Bidding = Current Purse - Reserved Fund
```

---

## Data Model (TypeScript)

```typescript
interface TeamPurseResponse {
  id: number;
  teamId: number;
  teamNumber: string;        // e.g., "T001"
  teamName: string;
  tournamentId: number;
  initialPurse: number;
  currentPurse: number;
  purseUsed: number;
  maxBidPerPlayer: number;
  reservedFund: number;
  availableForBidding: number;
  playersBought: number;
  remainingSlots: number;
}
```

---

## Common UI Patterns

### Pattern 1: Budget Alert
```typescript
// Warn if team is running low on budget
if (team.availableForBidding < team.initialPurse * 0.2) {
  displayWarning(`${team.teamName} budget running low!`);
}
```

### Pattern 2: Budget Depletion Message
```typescript
if (team.remainingSlots > 0 && team.availableForBidding < 50000) {
  displayAlert(`${team.teamName} may not fill remaining ${team.remainingSlots} slots`);
}
```

### Pattern 3: Auction Availability Check
```typescript
// Prevent bidding if no budget available
if (team.availableForBidding <= 0 && team.remainingSlots > 0) {
  disableBidButton(`${team.teamName} has no budget left`);
}
```

---

## Notes

1. **Purse updates automatically** - No need to manually trigger updates in backend
   - On player sale → purse decreases
   - On player unsold → purse increases
   - On tournament update → purses recalculated

2. **Reserved fund is non-negotiable** - Ensures teams can complete minimum squad

3. **Max bid prevents overspending** - Set at 25% to allow diverse purchases

4. **Real-time validation** - Always validate bid against `availableForBidding` and `maxBidPerPlayer`

5. **No manual purse management** - All updates are automatic through sale/unsold operations

---

## Integration Checklist

- [ ] Add TeamPurse endpoints to API client
- [ ] Create TeamPurse display component
- [ ] Add bid validation using `maxBidPerPlayer` and `availableForBidding`
- [ ] Refresh purse data after player sold/unsold
- [ ] Display budget warnings when purse runs low
- [ ] Show remaining slots and budget utilization
- [ ] Add real-time purse update notifications
- [ ] Test with various purse scenarios

---

## Support

For any issues:
1. Check backend logs for `TeamPurseService` errors
2. Verify team was created through API (purse initialized automatically)
3. Ensure tournament has `purseAmount`, `totalTeams`, and `playersPerTeam` set
4. Confirm JWT token has proper authorization

