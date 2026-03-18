# Team Purse Logic - Visual Guide

## Tournament Setup Example

```
Tournament Purse: 100,000
Total Teams: 5
Players per Team: 11

┌─────────────────────────────────────────────────────────────────┐
│ Tournament Purse Distribution                                   │
├─────────────────────────────────────────────────────────────────┤
│ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐              │
│ │   Team 1     │ │   Team 2     │ │   Team 3     │ ...          │
│ │ 20,000 each  │ │ 20,000 each  │ │ 20,000 each  │              │
│ └──────────────┘ └──────────────┘ └──────────────┘              │
│  ✅ Fair allocation (100,000 ÷ 5 = 20,000)                      │
└─────────────────────────────────────────────────────────────────┘
```

## Per-Team Budget Breakdown

```
Team's Initial Purse: 20,000
├─ Max Bid Per Player: 5,000 (25% protection)
│  └─ Prevents single huge purchase
│
└─ Reserved Fund: 5,454 (for minimum squad)
   ├─ Min Players to Reserve: 3 (11 ÷ 3)
   ├─ Cost per Player: 1,818 (20,000 ÷ 11)
   └─ Reserved = 1,818 × 3 = 5,454
      └─ Ensures team can buy 3 essential players
         and still have purse available for others

Available for Bidding (Initially): 14,546 (20,000 - 5,454)
```

## Purchase Flow Example

```
INITIAL STATE:
┌────────────────────────────────────────────────────────────────┐
│ initialPurse:       20,000                                      │
│ currentPurse:       20,000                                      │
│ purseUsed:              0                                       │
│ maxBidPerPlayer:     5,000 (25% limit)                          │
│ reservedFund:        5,454 (min squad reserve)                  │
│ availableForBidding: 14,546 ← Can bid up to this amount        │
│ playersBought:          0                                       │
│ remainingSlots:        11                                       │
└────────────────────────────────────────────────────────────────┘

PURCHASE 1: Buy Player A @ 4,500
┌─ Validation ─────────────────────────────────────────────────┐
│ ✅ 4,500 ≤ 5,000 (maxBidPerPlayer)                            │
│ ✅ 4,500 ≤ 14,546 (availableForBidding)                       │
│ ✅ APPROVED                                                    │
└───────────────────────────────────────────────────────────────┘

AFTER PURCHASE 1:
┌────────────────────────────────────────────────────────────────┐
│ initialPurse:       20,000                                      │
│ currentPurse:       15,500 (20,000 - 4,500) ↓                   │
│ purseUsed:           4,500 ↑                                    │
│ maxBidPerPlayer:     5,000 (unchanged)                          │
│ reservedFund:        5,454 (unchanged)                          │
│ availableForBidding: 10,046 (15,500 - 5,454) ← Reduced!        │
│ playersBought:          1 ↑                                     │
│ remainingSlots:        10 ↓                                     │
└────────────────────────────────────────────────────────────────┘

PURCHASE 2: Buy Player B @ 3,500
┌─ Validation ─────────────────────────────────────────────────┐
│ ✅ 3,500 ≤ 5,000 (maxBidPerPlayer)                            │
│ ✅ 3,500 ≤ 10,046 (availableForBidding)                       │
│ ✅ APPROVED                                                    │
└───────────────────────────────────────────────────────────────┘

AFTER PURCHASE 2:
┌────────────────────────────────────────────────────────────────┐
│ initialPurse:       20,000                                      │
│ currentPurse:       12,000 (15,500 - 3,500) ↓                   │
│ purseUsed:           8,000 ↑                                    │
│ maxBidPerPlayer:     5,000 (unchanged)                          │
│ reservedFund:        5,454 (unchanged)                          │
│ availableForBidding:  6,546 (12,000 - 5,454)                    │
│ playersBought:          2 ↑                                     │
│ remainingSlots:         9 ↓                                     │
└────────────────────────────────────────────────────────────────┘

PURCHASE 3: Try to buy Player C @ 4,000
┌─ Validation ─────────────────────────────────────────────────┐
│ ✅ 4,000 ≤ 5,000 (maxBidPerPlayer)                            │
│ ❌ 4,000 > 6,546 (availableForBidding) ← BLOCKED!             │
│ ❌ REJECTED - Insufficient available purse                    │
│                                                                │
│ Reason: Must reserve 5,454 for minimum squad!                 │
│         Only 6,546 available (12,000 - 5,454)                 │
└───────────────────────────────────────────────────────────────┘
```

## Key Difference: OLD vs NEW Logic

```
╔════════════════════════════════════════════════════════════════╗
║ OLD LOGIC (WRONG) ❌                                           ║
╟────────────────────────────────────────────────────────────────╢
║ Team Purchases:      4,500                                     ║
║ Check: 4,500 ≤ tournament.purseAmount - totalUsedByAllTeams    ║
║ Problem: Used TOTAL tournament purse (100,000)!                ║
║          Teams could collectively spend 100,000, not 20k each! ║
║ Result: Unfair allocation & impossible budget limits           ║
╚════════════════════════════════════════════════════════════════╝

╔════════════════════════════════════════════════════════════════╗
║ NEW LOGIC (CORRECT) ✅                                         ║
╟────────────────────────────────────────────────────────────────╢
║ Team Purchases:      4,500                                     ║
║ Check 1: 4,500 ≤ maxBidPerPlayer (5,000)                       ║
║   ✅ Prevents single huge purchase (diversity)                 ║
║                                                                ║
║ Check 2: 4,500 ≤ availableForBidding (14,546)                  ║
║   ✅ Respects team's fair share (20,000)                       ║
║   ✅ Respects reserved fund (5,454)                            ║
║                                                                ║
║ Result: Fair, consistent, and balanced allocation             ║
╚════════════════════════════════════════════════════════════════╝
```

## Summary of Fixes

| Aspect | Before | After |
|--------|--------|-------|
| **Purse Calculation** | Total tournament purse ❌ | Team's share (purse ÷ teams) ✅ |
| **Max Bid Limit** | Not enforced ❌ | Enforced at 25% ✅ |
| **Reserved Fund** | Ignored ❌ | Enforced in availableForBidding ✅ |
| **Balance Check** | Against total | Against team's allocated ✅ |
| **Fairness** | Unfair ❌ | Fair and equal ✅ |

## Impact

- ✅ Each team gets equal initial purse
- ✅ Purse reduces correctly as players are bought
- ✅ Reserved fund ensures minimum squad completion
- ✅ Max bid prevents monopoly on expensive players
- ✅ Transparent and predictable bidding limits

