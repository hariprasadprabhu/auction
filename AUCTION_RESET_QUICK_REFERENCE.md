# Auction Reset API - Quick Reference

## 📌 Overview
Two new endpoints to reset auctions with automatic tournament settings recalculation.

---

## 🔗 API Endpoints

### 1. Reset Specific Players
```
POST /tournaments/{tournamentId}/auction-players/reset
Content-Type: application/json

{
  "playerIds": [1, 2, 3, 4]
}
```
**Response:** `{ "processedCount": 4, "skippedCount": 0, "totalRequested": 4 }`

### 2. Reset Entire Auction
```
POST /tournaments/{tournamentId}/auction/reset-entire
```
**Response:**
```json
{
  "deletedAuctionPlayers": 45,
  "readdedApprovedPlayers": 45,
  "teamsReset": 10,
  "appliedTournamentSettings": {
    "purseAmount": 5000000,
    "playersPerTeam": 11,
    "basePrice": 50000
  }
}
```

---

## 🎯 When to Use

### Reset Specific Players
- ✅ Player status changed from REJECTED → APPROVED
- ✅ Add specific players back to auction
- ✅ Remove specific sold players from auction
- ✅ Fix individual player auctions

### Reset Entire Auction
- ✅ Start fresh re-auction
- ✅ Tournament settings changed
- ✅ Systematic errors in initial setup
- ✅ New auction round with same players

---

## ⚙️ What Gets Recalculated

### Reset Specific Players
For each player:
- ✅ Refunds sold price to team
- ✅ Recalculates team purse, maxBid, reserved amount
- ✅ Status: APPROVED → UPCOMING (available)
- ✅ Status: NOT APPROVED → UNSOLD (unavailable)

### Reset Entire Auction
For all teams:
- ✅ Refunds ALL sold players
- ✅ Resets `initialPurse` = current tournament purseAmount
- ✅ Resets `remainingSlots` = current tournament playersPerTeam
- ✅ Recalculates `reservedFund` = (remainingSlots - 1) × current basePrice
- ✅ Recalculates `maxBidPerPlayer` = currentPurse - reservedFund
- ✅ Clears `purseUsed`, `playersBought`

For all auction players:
- ✅ Old auction entries deleted
- ✅ Approved players re-added with current `basePrice`
- ✅ Sort order reset from 1

---

## 🚀 Quick Examples

### Curl: Reset 3 Players
```bash
curl -X POST http://localhost:8080/tournaments/1/auction-players/reset \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"playerIds": [10, 11, 12]}'
```

### Curl: Reset Entire Auction
```bash
curl -X POST http://localhost:8080/tournaments/1/auction/reset-entire \
  -H "Authorization: Bearer TOKEN"
```

### JavaScript/Fetch
```javascript
// Reset specific players
const response = await fetch('/tournaments/1/auction-players/reset', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer TOKEN'
  },
  body: JSON.stringify({
    playerIds: [10, 11, 12]
  })
});

// Reset entire auction
const response = await fetch('/tournaments/1/auction/reset-entire', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer TOKEN'
  }
});
```

### Postman
1. **Create Request:** POST
2. **URL:** `{{BASE_URL}}/tournaments/{{tournamentId}}/auction-players/reset`
3. **Headers:** `Authorization: Bearer {{TOKEN}}`
4. **Body (JSON):**
   ```json
   {
     "playerIds": [1, 2, 3]
   }
   ```
5. **Send**

---

## 📊 Data Flow

### Reset Specific Players
```
Request (playerIds)
    ↓
Ownership Check
    ↓
For each player:
  ├─ Check if sold → Refund to team
  ├─ Check status → Set UPCOMING or UNSOLD
  └─ Save changes
    ↓
Response (processedCount, skippedCount)
```

### Reset Entire Auction
```
Request
    ↓
Ownership Check
    ↓
Step 1: Refund all sold players
    ↓
Step 2: Delete old auction pool
    ↓
Step 3: Re-add approved players (with current basePrice)
    ↓
Step 4: Reset all team purses (with current settings)
    ↓
Response (deletedCount, readiedCount, teamsReset, appliedSettings)
```

---

## ✅ Verification

After Reset Specific Players:
```sql
-- Check player status
SELECT id, auctionStatus, soldToTeam, soldPrice FROM auction_players 
WHERE id IN (10, 11, 12);

-- Check team purse updated
SELECT maxBidPerPlayer, reservedFund FROM team_purse 
WHERE team_id = ?;
```

After Reset Entire Auction:
```sql
-- Check auction players re-added
SELECT COUNT(*) FROM auction_players 
WHERE tournament_id = ? AND auctionStatus = 'UPCOMING';

-- Check team purses reset
SELECT initialPurse, currentPurse, purseUsed, maxBidPerPlayer 
FROM team_purse WHERE tournament_id = ?;

-- Should all have: purseUsed=0, currentPurse=initialPurse
```

---

## ⚠️ Important Notes

- **Ownership Required:** Only tournament owner can reset
- **Data Loss:** Old auction entries deleted (not archived)
- **No Player Loss:** Player registrations preserved
- **Transactional:** All-or-nothing (no partial updates)
- **Atomic:** All changes commit together
- **Reversible:** Can reset multiple times

---

## 🔍 Response Codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 400 | Bad request (invalid body) |
| 401 | Unauthorized (no auth) |
| 403 | Forbidden (not owner) |
| 404 | Tournament not found |
| 500 | Server error |

---

## 💡 Tips

1. **Check response** for `appliedTournamentSettings` to verify correct settings used
2. **Always verify** team purses after reset
3. **Reset during off-peak** for large tournaments
4. **Backup important** auction data before reset (if needed)
5. **Use specific reset** for targeted fixes
6. **Use entire reset** for fresh start

---

## 📚 Full Documentation

- API Details: `AUCTION_RESET_API_DOCUMENTATION.md`
- Technical: `TOURNAMENT_SETTINGS_DYNAMIC_RECALCULATION.md`
- Implementation: `AUCTION_RESET_IMPLEMENTATION_SUMMARY.md`
- Complete Guide: `AUCTION_RESET_COMPLETE_GUIDE.md`

