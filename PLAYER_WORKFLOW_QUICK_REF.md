# Quick Developer Reference: Player Workflow

## The Rule (One Sentence)
**"When a Player is added, they go to the Players list with PENDING status. Only APPROVED players can enter the Auction."**

---

## Code Files Involved

```
src/main/java/com/bid/auction/
├── entity/
│   ├── Player.java                 ← Player registration (status: PENDING/APPROVED/REJECTED)
│   ├── AuctionPlayer.java          ← Auction participant
│   ├── Tournament.java             ← Contains lists of both
│   └── User.java                   ← Tournament owner
├── enums/
│   ├── PlayerStatus.java           ← PENDING, APPROVED, REJECTED
│   └── AuctionStatus.java          ← UPCOMING, SOLD, UNSOLD
├── repository/
│   ├── PlayerRepository.java       ← Queries for Players
│   └── AuctionPlayerRepository.java ← Queries for AuctionPlayers
├── service/
│   ├── PlayerService.java          ← Registration, approval, stats
│   ├── AuctionPlayerService.java   ← Auction pool management
│   └── TournamentService.java      ← Owner verification
└── controller/
    ├── PlayerController.java       ← Player endpoints
    └── AuctionPlayerController.java ← Auction endpoints
```

---

## Key Methods in PlayerService

### Register a Player
```java
public PlayerResponse register(Long tournamentId, PlayerRegisterRequest req)
// Creates new Player with status = PENDING
// Adds to tournament.players automatically
```

### Approve Player
```java
public Map<String, Object> approve(Long id, User user)
// Changes status: PENDING → APPROVED
// Now eligible for auction
```

### Reject Player
```java
public Map<String, Object> reject(Long id, User user)
// Changes status: PENDING → REJECTED
// Cannot enter auction
```

### Get Approved Players
```java
public List<PlayerResponse> getApprovedByTournament(Long tournamentId, User user)
// Returns only players with status = APPROVED
// These are ready for auction
```

### Get Player Statistics
```java
public Map<String, Object> getPlayerStatsByTournament(Long tournamentId, User user)
// Returns counts: {totalPlayers, pending, approved, rejected}
```

---

## Key Method in AuctionPlayerService

### THE ENFORCER: Promote to Auction
```java
public AuctionPlayerResponse promoteToAuction(Long playerId, AddToAuctionRequest req, User user)
// ★ KEY VALIDATION ★
// if (player.getStatus() != PlayerStatus.APPROVED)
//     throw new IllegalArgumentException("Must be APPROVED!")
//
// Only runs if validation passes:
// 1. Creates new AuctionPlayer record
// 2. Copies name, playerNumber, role, photo from Player
// 3. Adds cricketStats (age, city, batting style, etc.)
// 4. Sets auctionStatus = UPCOMING
// 5. Adds to tournament.auctionPlayers
```

---

## Database Tables

### players table
```
| id | player_number | first_name | last_name | role | status  | tournament_id | photo | payment_proof | created_at |
|----|---|---|---|---|---|---|---|---|---|
| 1  | P001 | John | Doe | Batsman | APPROVED | 10 | [bytes] | [bytes] | 2024-01-15 |
| 2  | P002 | Jane | Smith | Bowler | PENDING | 10 | [bytes] | [bytes] | 2024-01-15 |
| 3  | P003 | Bob | Johnson | Batsman | REJECTED | 10 | [bytes] | [bytes] | 2024-01-15 |
```

### auction_players table
```
| id | player_id | player_number | first_name | role | base_price | auction_status | tournament_id | sold_to_team_id | sold_price | sort_order | created_at |
|----|---|---|---|---|---|---|---|---|---|---|---|
| 50 | 1 | P001 | John | Batsman | 100000 | UPCOMING | 10 | NULL | NULL | 1 | 2024-01-16 |
| 51 | NULL | P004 | Admin Added | Bowler | 80000 | UPCOMING | 10 | NULL | NULL | 2 | 2024-01-16 |
```

**Note:** `player_id` can be NULL for admin-created auction players

---

## The Flow in 4 Steps

```
┌────────────────────────────────────────────────────────────┐
│ STEP 1: Player Registers                                  │
│ POST /api/players/register/{tournamentId}                │
│ → Player table: status = PENDING                          │
└────────────────────────────────────────────────────────────┘
                          ↓
┌────────────────────────────────────────────────────────────┐
│ STEP 2: Tournament Owner Approves                         │
│ PATCH /api/players/{id}/approve                          │
│ → Player table: status = PENDING → APPROVED              │
└────────────────────────────────────────────────────────────┘
                          ↓
┌────────────────────────────────────────────────────────────┐
│ STEP 3: Add Approved Player to Auction                   │
│ POST /api/players/{id}/add-to-auction                    │
│                                                           │
│ VALIDATION CHECK:                                        │
│ if (status != APPROVED) {                               │
│     throw new IllegalArgumentException(...)             │
│ }                                                        │
│                                                           │
│ → AuctionPlayer table: NEW row created                   │
│ → auctionStatus = UPCOMING                               │
└────────────────────────────────────────────────────────────┘
                          ↓
┌────────────────────────────────────────────────────────────┐
│ STEP 4: Auction Happens                                   │
│ PATCH /api/auction-players/{id}/sell                     │
│ → AuctionPlayer table: auctionStatus = SOLD              │
│ → soldToTeamId = Team ID                                 │
│ → soldPrice = Price paid                                 │
└────────────────────────────────────────────────────────────┘
```

---

## Critical Validations

### In AuctionPlayerService.promoteToAuction()

```java
// 1. Player exists
Player player = playerRepository.findById(playerId)
    .orElseThrow(...);

// 2. User owns the tournament
Tournament tournament = tournamentService.findAndVerifyOwner(...);

// 3. ★ STATUS CHECK - KEY RULE ★
if (player.getStatus() != PlayerStatus.APPROVED) {
    throw new IllegalArgumentException(
        "Player '" + player.getFirstName() + "' is not APPROVED"
    );
}

// 4. Not already in auction
if (auctionPlayerRepository.existsByPlayerIdAndTournamentId(...)) {
    throw new IllegalArgumentException(
        "Player already in auction pool"
    );
}
```

---

## Query Methods

### Find Approved Players (for auction eligibility)
```java
// In PlayerRepository
List<Player> findByTournamentAndStatus(Tournament tournament, PlayerStatus status)

// Usage
List<Player> approved = playerRepository
    .findByTournamentAndStatus(tournament, PlayerStatus.APPROVED);
```

### Find Players by Status
```java
// In PlayerRepository
List<Player> findByTournamentAndStatus(Tournament tournament, PlayerStatus status)

// Usage (PENDING, APPROVED, or REJECTED)
List<Player> pending = playerRepository
    .findByTournamentAndStatus(tournament, PlayerStatus.PENDING);
```

### Check if Player Already in Auction
```java
// In AuctionPlayerRepository
boolean existsByPlayerIdAndTournamentId(Long playerId, Long tournamentId)

// Usage
if (auctionPlayerRepository.existsByPlayerIdAndTournamentId(playerId, tournamentId)) {
    // Player already auctioned for this tournament
}
```

---

## New Features Added

### 1. Get Approved Players Only
```
GET /api/tournaments/{tournamentId}/players/approved
Authorization: Bearer <token>

Response:
[
  {
    "id": 1,
    "playerNumber": "P001",
    "firstName": "John",
    "status": "APPROVED",
    ...
  }
]
```

### 2. Get Player Statistics
```
GET /api/tournaments/{tournamentId}/players/stats
Authorization: Bearer <token>

Response:
{
  "totalPlayers": 100,
  "pending": 45,
  "approved": 50,
  "rejected": 5
}
```

---

## Testing Scenarios

### ✅ Success Case
```
1. POST /api/players/register/10
   → {id: 1, status: "PENDING"}

2. PATCH /api/players/1/approve
   → {status: "APPROVED"}

3. POST /api/players/1/add-to-auction
   → {id: 50, auctionStatus: "UPCOMING"}

4. PATCH /api/auction-players/50/sell
   → {auctionStatus: "SOLD", soldPrice: 150000}
```

### ❌ Failure Case: Pending Player
```
1. POST /api/players/register/10
   → {id: 2, status: "PENDING"}

2. POST /api/players/2/add-to-auction  ← WITHOUT approving first
   → ERROR: "Player is not APPROVED"
```

### ❌ Failure Case: Rejected Player
```
1. POST /api/players/register/10
   → {id: 3, status: "PENDING"}

2. PATCH /api/players/3/reject
   → {status: "REJECTED"}

3. POST /api/players/3/add-to-auction
   → ERROR: "Player is not APPROVED (current status: REJECTED)"
```

### ✅ Admin Creates Auction Player Directly
```
POST /api/tournaments/10/auction-players
{
  "playerNumber": "A001",
  "firstName": "Direct",
  "role": "Batsman",
  "basePrice": 100000,
  ...
}
→ {id: 60, playerId: null, auctionStatus: "UPCOMING"}
```

---

## Where the Magic Happens

**The enforcement of "only APPROVED players can auction":**

File: `AuctionPlayerService.java`, Line ~100-115

```java
@Transactional
public AuctionPlayerResponse promoteToAuction(Long playerId, AddToAuctionRequest req, User user) {
    // ... other validations ...
    
    // ⭐ THIS IS THE KEY CHECK ⭐
    if (player.getStatus() != PlayerStatus.APPROVED) {
        throw new IllegalArgumentException(
                "Player '" + player.getFirstName() + " " + player.getLastName()
                        + "' is not APPROVED (current status: " + player.getStatus() + "). "
                        + "Approve the player before adding to auction.");
    }
    
    // ... create AuctionPlayer ...
}
```

---

## Summary Checklist

- [x] Players automatically added to tournament.players list upon creation
- [x] Initial status is PENDING
- [x] Tournament owner can approve/reject players
- [x] Only APPROVED players can enter auction
- [x] Validation enforced in promoteToAuction() method
- [x] AuctionPlayer created with reference to original Player
- [x] Two-table approach: players (registration) + auction_players (bidding)
- [x] New endpoints for stats and approved players
- [x] All error messages are clear and actionable


