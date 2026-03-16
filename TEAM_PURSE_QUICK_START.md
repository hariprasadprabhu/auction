# Team Purse - Quick Start Guide

## 🎯 What You Have

A complete **Team Financial Management System** that tracks:
- Current purse (remaining budget)
- Max bid per player (safety limit)
- Reserved fund (minimum squad protection)
- Available for bidding (actual spendable amount)

## 📍 5-Minute Integration

### Step 1: Fetch Team Purses
```typescript
// When loading tournament auction screen
const purses = await http.get(`/api/tournaments/${tournamentId}/team-purses`);
console.log(purses);
// [
//   {
//     teamId: 1,
//     teamNumber: "T001",
//     teamName: "Team A",
//     currentPurse: 750000,
//     availableForBidding: 386364,
//     maxBidPerPlayer: 250000,
//     playersBought: 2,
//     remainingSlots: 9
//   },
//   ...
// ]
```

### Step 2: Display Budget Info
```html
<div class="team-budget">
  <h3>{{ team.teamName }} ({{ team.teamNumber }})</h3>
  
  <!-- Main Display -->
  <div class="budget-display">
    <p>Available Budget: <strong>₹{{ team.availableForBidding | currency }}</strong></p>
    <p>Max Bid Per Player: ₹{{ team.maxBidPerPlayer | currency }}</p>
  </div>
  
  <!-- Progress -->
  <div class="progress">
    <p>Squad Progress: {{ team.playersBought }}/{{ team.playersBought + team.remainingSlots }}</p>
    <progress value="{{ team.playersBought }}" max="{{ team.playersBought + team.remainingSlots }}"></progress>
  </div>
</div>
```

### Step 3: Validate Bid
```typescript
// When user tries to place a bid
function validateBid(bidAmount: number, team: TeamPurse): boolean {
  if (bidAmount > team.maxBidPerPlayer) {
    alert(`Max bid allowed: ₹${team.maxBidPerPlayer}`);
    return false;
  }
  if (bidAmount > team.availableForBidding) {
    alert(`Available budget: ₹${team.availableForBidding}`);
    return false;
  }
  return true;
}
```

### Step 4: Refresh After Sale
```typescript
// After player is sold
async function onPlayerSold(teamId: number, tournamentId: number, soldPrice: number) {
  // Backend automatically updates purse, just fetch fresh data
  const updatedPurse = await http.get(
    `/api/tournaments/${tournamentId}/teams/${teamId}/purse`
  );
  
  // Update UI
  displayBudget(updatedPurse);
  showNotification(`Budget updated! Available: ₹${updatedPurse.availableForBidding}`);
}
```

## 🔑 Key Fields

| Field | What to Show | Example |
|-------|---|---|
| `availableForBidding` | **Actual budget for bidding** | Use this for bid validation |
| `maxBidPerPlayer` | Max single bid | ₹250,000 |
| `currentPurse` | Total left | ₹750,000 (informational) |
| `playersBought` | Players acquired | Show as "2/11" |
| `remainingSlots` | Slots to fill | "9 more slots" |

## 🌐 API Endpoints

### Get All Team Purses (Dashboard)
```
GET /api/tournaments/{tournamentId}/team-purses
```
Returns array of all team purses for a tournament

### Get Specific Team Purse
```
GET /api/tournaments/{tournamentId}/teams/{teamId}/purse
```
Returns single team's purse details

### Get Team Across Tournaments
```
GET /api/teams/{teamId}/purses
```
Returns team's purse info across all tournaments

## ✅ Automatic Updates (No Backend Calls Needed!)

These happen automatically in backend:

1. **Team Created** → Purse initialized with calculations
2. **Player Sold** → Purse updated (spent increases, available decreases)
3. **Player Unsold** → Purse reverted (spent decreases, available increases)
4. **Tournament Changed** → All purses recalculated

**You only need to call GET endpoints to refresh UI!**

## 📊 Example Response

```json
{
  "id": 5,
  "teamId": 10,
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

## 🎬 Usage Timeline

```
1. Auction Page Load
   ├─ GET /api/tournaments/{id}/team-purses
   └─ Display all teams' budgets

2. Before Player Bid
   ├─ Check team.maxBidPerPlayer
   └─ Check team.availableForBidding

3. After Player Sold
   ├─ GET /api/tournaments/{id}/teams/{teamId}/purse (refresh)
   └─ Update UI with new budget

4. Tournament Updated
   ├─ Backend auto-recalculates
   └─ GET /api/tournaments/{id}/team-purses (refresh)
```

## 🚨 Common Scenarios

### Budget Running Low
```typescript
if (team.availableForBidding < 100000) {
  showWarning(`${team.teamName} budget critically low!`);
}
```

### Team Cannot Complete Squad
```typescript
if (team.remainingSlots > 0 && team.availableForBidding < 50000) {
  showAlert(`${team.teamName} may not fill all ${team.remainingSlots} slots`);
}
```

### Disable Bidding if No Budget
```typescript
if (team.availableForBidding <= 0) {
  disableBidButton(`${team.teamName} has no budget`);
}
```

## 📋 Integration Checklist

- [ ] Add TeamPurse interface to your models
- [ ] Create HTTP service method for team purses
- [ ] Add purse display component
- [ ] Add bid validation logic
- [ ] Add refresh after player sale
- [ ] Test with multiple scenarios
- [ ] Display budget warnings
- [ ] Show squad completion status

## 💡 Pro Tips

1. **Cache purse data** - Store it in component to reduce API calls
2. **Real-time updates** - Refresh after each player transaction
3. **Visual feedback** - Use progress bars for squad status
4. **Budget alerts** - Warn when running low
5. **Disable bidding** - When available budget is zero

## 📚 Full Documentation

- `TEAM_PURSE_IMPLEMENTATION.md` - Complete technical details
- `TEAM_PURSE_UI_INTEGRATION.md` - Detailed UI examples
- `TEAM_PURSE_DATABASE_SCHEMA.md` - Database details

## ❓ FAQ

**Q: When is purse updated?**
A: Automatically when player is sold/unsold. Just fetch fresh data via GET endpoint.

**Q: What if team runs out of budget?**
A: Reserved fund ensures minimum squad completion. Available for bidding will show 0.

**Q: Can I manually update purse?**
A: No - all updates are automatic through player transactions and tournament changes.

**Q: How often should I refresh?**
A: After every player sale/unsold event and tournament edit.

**Q: What should I use for bid validation?**
A: Always compare against `availableForBidding` and `maxBidPerPlayer`.

---

**You're ready to go! Start with fetching team purses and displaying them on your auction screen.**

