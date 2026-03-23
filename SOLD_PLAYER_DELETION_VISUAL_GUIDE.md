# SOLD Player Deletion - Visual Guide

## 🎯 Feature Overview

```
┌─────────────────────────────────────────────────────────────┐
│  SOLD Player Deletion with Team Purse Recalculation        │
│                                                             │
│  When a SOLD player is deleted:                            │
│  1. Refund team purse                                      │
│  2. Recalculate all team financial values                  │
│  3. Delete player and auction records                      │
│  4. Keep transaction atomic and consistent                 │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔄 Process Flow Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│ User: DELETE /api/players/123 (Player Status = SOLD)             │
│ Authorization: Bearer {token}                                     │
└──────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│ PlayerService.delete(123, user)                                  │
│                                                                   │
│ ✓ Find player by ID                                              │
│ ✓ Verify tournament ownership (user is tournament owner)         │
│ ✓ Get tournament ID                                              │
└──────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│ AuctionPlayerService.deletePlayerWithAuctionRefunds(123)        │
│ (Calls removeFromAuctionIfPresent)                              │
│                                                                   │
│ ✓ Find all auction players linked to player 123                 │
│ ✓ Iterate through each auction player record                    │
└──────────────────────────────────────────────────────────────────┘
                           │
                           ▼
        ┌──────────────────────────────────────┐
        │ For Each AuctionPlayer:              │
        │                                       │
        │ Is it SOLD?                          │
        │ (soldToTeam != null &&               │
        │  soldPrice != null)                  │
        └──────────────────────────────────────┘
                    │              │
              YES  ▼              ▼  NO
         ┌────────────────┐  ┌──────────┐
         │ YES - REFUND   │  │ NO-SKIP  │
         │ TEAM           │  │          │
         └────────────────┘  └──────────┘
                │                │
                └────────┬───────┘
                         ▼
    ┌────────────────────────────────────────┐
    │ For SOLD players:                      │
    │ TeamPurseService                       │
    │ .updatePurseOnPlayerUnsold(            │
    │    team,                               │
    │    tournament,                         │
    │    soldPrice                           │
    │ )                                      │
    └────────────────────────────────────────┘
                    │
    ┌───────────────┼────────────────────┐
    │               │                    │
    ▼               ▼                    ▼
 REFUND        RECALCULATE         RECALCULATE
 PURSE         PLAYER COUNTS       FUND VALUES
 
 purseUsed     playersBought       reservedFund
 = max(0,      = max(0,           = (remainingSlots-1)
   purseUsed     playersBought     × basePrice
   - price)     - 1)              
 
 currentPurse   remainingSlots     maxBidPerPlayer
 = initial      = remaining        = currentPurse
   - purseUsed  + 1                - reservedFund
 
                                  availableForBidding
                                  = currentPurse
                                    - reservedFund
```

---

## 📊 Before & After Values

```
SCENARIO: Delete player sold for ₹100,000 to Team A
Tournament: basePrice=₹5000, playersPerTeam=11

┌─────────────────────────────────────────────────────────────┐
│                    BEFORE DELETION                           │
├─────────────────────────────────────────────────────────────┤
│ Team A Purse Details:                                        │
│                                                              │
│  Initial Purse        ₹1,000,000                             │
│  Purse Used           ₹  250,000  (5 players)                │
│  Current Purse        ₹  750,000                             │
│                                                              │
│  Players Bought       5                                      │
│  Remaining Slots      6 (11 - 5)                             │
│                                                              │
│  Reserved Fund        ₹25,000    (5 × ₹5000)                 │
│  Max Bid/Player       ₹725,000   (₹750k - ₹25k)              │
│  Available for Bid    ₹725,000   (₹750k - ₹25k)              │
└─────────────────────────────────────────────────────────────┘

                        ↓ DELETE SOLD PLAYER (₹100,000) ↓

┌─────────────────────────────────────────────────────────────┐
│                    AFTER DELETION                            │
├─────────────────────────────────────────────────────────────┤
│ Team A Purse Details:                                        │
│                                                              │
│  Initial Purse        ₹1,000,000                             │
│  Purse Used           ₹  150,000  (4 players) ⬆️ Refunded   │
│  Current Purse        ₹  850,000  ⬆️ Increased              │
│                                                              │
│  Players Bought       4           ⬇️ Decremented            │
│  Remaining Slots      7 (11 - 4)  ⬆️ Incremented            │
│                                                              │
│  Reserved Fund        ₹30,000     ⬆️ Recalc (6×₹5000)       │
│  Max Bid/Player       ₹820,000    ⬆️ Recalc (₹850k-₹30k)    │
│  Available for Bid    ₹820,000    ⬆️ Recalc (₹850k-₹30k)    │
└─────────────────────────────────────────────────────────────┘

Changes:
  ✅ Purse Used:        ₹250,000 → ₹150,000  (-₹100,000)
  ✅ Current Purse:     ₹750,000 → ₹850,000  (+₹100,000)
  ✅ Players Bought:    5 → 4                 (-1)
  ✅ Remaining Slots:   6 → 7                 (+1)
  ✅ Reserved Fund:     ₹25,000 → ₹30,000    (recalculated)
  ✅ Max Bid/Player:    ₹725,000 → ₹820,000  (recalculated)
  ✅ Available for Bid: ₹725,000 → ₹820,000  (recalculated)
```

---

## 🗂️ Class Diagram

```
┌──────────────────────────────────────────┐
│            PlayerService                  │
├──────────────────────────────────────────┤
│ - playerRepository                        │
│ - auctionPlayerService                    │
│ - tournamentService                       │
├──────────────────────────────────────────┤
│ + delete(id, user)          ◄──── DELETE │
│ + update(id, req, user)                   │
│ + approve(id, user)                       │
│ + reject(id, user)                        │
└──────────────────┬───────────────────────┘
                   │ calls
                   ▼
┌──────────────────────────────────────────┐
│        AuctionPlayerService               │
├──────────────────────────────────────────┤
│ - auctionPlayerRepository                 │
│ - playerRepository                        │
│ - teamPurseService                        │
├──────────────────────────────────────────┤
│ + removeFromAuctionIfPresent(playerId)   │
│ + deletePlayerWithAuctionRefunds(id)     │
│ + sell(id, request, user)                 │
│ + markUnsold(id, user)                    │
└──────────────────┬───────────────────────┘
                   │ calls
                   ▼
┌──────────────────────────────────────────┐
│       TeamPurseService                    │
├──────────────────────────────────────────┤
│ - teamPurseRepository                     │
├──────────────────────────────────────────┤
│ + updatePurseOnPlayerSold(...)            │
│ + updatePurseOnPlayerUnsold(...)    ◄────┤
│ + recalculateAllTeamPurses(...)           │
└──────────────────────────────────────────┘
```

---

## 📈 State Transitions

```
Player States:
  PENDING ──────── APPROVED ──────── UNSOLD
    │                  │
    │                  └────── SOLD ───┐
    │                                  │
    └──────────── REJECTED             │
                                       │
  When SOLD player is deleted: ───────┘
         Removes record, refunds team

Team Purse State:
  Initial State
      │
      ├─ Player 1 SOLD (+₹100,000)
      │  currentPurse: ₹1,000,000 → ₹900,000
      │
      ├─ Player 2 SOLD (+₹50,000)
      │  currentPurse: ₹900,000 → ₹850,000
      │
      ├─ Delete Player 2 (-₹50,000)  ◄──── REFUND
      │  currentPurse: ₹850,000 → ₹900,000
      │
      └─ Delete Player 1 (-₹100,000) ◄──── REFUND
         currentPurse: ₹900,000 → ₹1,000,000
```

---

## 🔐 Transaction Safety

```
┌─ BEGIN TRANSACTION ─────────────────────────┐
│                                              │
│  Step 1: Verify Ownership ✓                 │
│  Step 2: Refund purseUsed ✓                 │
│  Step 3: Update currentPurse ✓              │
│  Step 4: Update playersBought ✓             │
│  Step 5: Update remainingSlots ✓            │
│  Step 6: Recalc reservedFund ✓              │
│  Step 7: Recalc maxBidPerPlayer ✓           │
│  Step 8: Recalc availableForBidding ✓       │
│  Step 9: Save TeamPurse ✓                   │
│  Step 10: Delete AuctionPlayer ✓            │
│  Step 11: Delete Player ✓                   │
│                                              │
│  All succeed? ──→ COMMIT ✓                  │
│  Any fail?   ──→ ROLLBACK (undo all)        │
│                                              │
└─ END TRANSACTION ──────────────────────────┘
```

---

## ✅ Validation Checks

```
Before Deletion:
  ✓ Player exists
  ✓ Tournament exists
  ✓ User is tournament owner
  ✓ Auction player linked to player
  ✓ Team purse exists

During Deletion:
  ✓ Sold status verified (soldToTeam != null)
  ✓ Sold price not null (soldPrice != null)
  ✓ Base price has default (5000) if missing
  ✓ All math operations use Math.max() for safety

After Deletion:
  ✓ currentPurse = initialPurse - purseUsed
  ✓ No negative values in purse fields
  ✓ Remaining slots >= 0
  ✓ Players bought >= 0
  ✓ Records deleted from database
```

---

## 📋 Testing Matrix

```
Scenario 1: Single SOLD Player
  Team: 5/11 players, ₹750k remaining
  Delete: Player sold for ₹100k
  Result: ₹850k remaining, 4/11 players ✓

Scenario 2: Multiple SOLD Players
  Team: 5/11 players
  Delete: Player 1 (₹100k), Player 2 (₹50k)
  Result: All values recalculated for each ✓

Scenario 3: Edge Case - Only Player
  Team: 1/11 players
  Delete: Only SOLD player
  Result: Purse fully refunded, 0/11 players ✓

Scenario 4: Non-SOLD Player
  Team: 5/11 players
  Delete: Unsold player
  Result: No refund, no recalc, just delete ✓

Scenario 5: Missing Team Purse
  Team: Exists, purse deleted
  Delete: SOLD player
  Result: Transaction rollback, error ✓
```

---

## 🚀 API Integration

```
Frontend Request:
  DELETE /api/players/123
  Headers: {
    "Authorization": "Bearer eyJhbGc..."
  }

Backend Processing:
  1. Extract token → Get current user
  2. Find player 123
  3. Get tournament from player
  4. Verify user owns tournament
  5. Call deletePlayerWithAuctionRefunds(123)
     ├─ Find auction players by playerId
     ├─ For each SOLD:
     │  └─ updatePurseOnPlayerUnsold(team, tourn, price)
     └─ Delete auction players
  6. Delete player
  7. Commit transaction

Frontend Response:
  204 No Content
  
Query updated team purse:
  GET /api/teams/{teamId}/purse/{tournamentId}
  Response: {
    "currentPurse": 850000,
    "playersBought": 4,
    "remainingSlots": 7,
    "maxBidPerPlayer": 820000,
    "availableForBidding": 820000,
    ...
  }
```

---

## 📝 Code References

| Aspect | File | Method | Line |
|--------|------|--------|------|
| Player Delete | PlayerService.java | delete() | ~91 |
| Remove from Auction | AuctionPlayerService.java | removeFromAuctionIfPresent() | ~110 |
| Refund & Recalc | TeamPurseService.java | updatePurseOnPlayerUnsold() | ~61 |
| Repository | AuctionPlayerRepository.java | findByPlayerId() | - |

---

## 🎯 Summary

```
    Input: DELETE /api/players/{id}
           where player.status = SOLD
    
    Process: 
      → Verify ownership
      → Find auction record
      → Refund team purse
      → Recalculate all values
      → Delete records
      → Commit transaction
    
    Output: 204 No Content
            Team values updated in DB
```

This visual guide helps understand the complete flow and data transformations involved in the SOLD player deletion feature.

