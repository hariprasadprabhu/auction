# Player Workflow - Data Flow Architecture

## Overview
This document describes the complete workflow for how players are registered, approved, and participate in auctions.

---

## Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          PLAYER LIFECYCLE FLOW                              │
└─────────────────────────────────────────────────────────────────────────────┘

STAGE 1: PLAYER REGISTRATION
┌─────────────────────────────┐
│  Player Self-Registers      │
│  or Admin Adds Player       │
└──────────────┬──────────────┘
               │
               ▼
┌──────────────────────────────────────────────────────────────────┐
│ NEW PLAYER CREATED IN PLAYERS LIST WITH STATUS = PENDING        │
├──────────────────────────────────────────────────────────────────┤
│ Entity: Player                                                   │
│ - id: Long                                                       │
│ - playerNumber: String (e.g., "P001", "P002")                 │
│ - firstName: String                                              │
│ - lastName: String                                               │
│ - dob: LocalDate                                                 │
│ - role: String (e.g., "Batsman", "Bowler")                    │
│ - status: PlayerStatus.PENDING (initially)                      │
│ - tournament: Tournament (many players per tournament)           │
│ - photo: byte[] (optional)                                       │
│ - paymentProof: byte[] (optional)                                │
│ - createdAt: LocalDateTime (auto-set)                            │
└──────────────────────────────┬───────────────────────────────────┘
                               │
                               ▼
STAGE 2: TOURNAMENT OWNER APPROVAL
┌──────────────────────────────────────────────────────────────────┐
│ Tournament Owner Reviews Player Registration                     │
│ OPTIONS:                                                         │
│   1. APPROVE ✓                                                   │
│   2. REJECT ✗                                                    │
└──────────────────────────────┬───────────────────────────────────┘
                               │
                ┌──────────────┴──────────────┐
                │                             │
                ▼                             ▼
      ✓ APPROVED                    ✗ REJECTED
      Player Status:                Player Status:
      APPROVED                      REJECTED
                │                         │
                │                         └─→ [END - Cannot participate]
                │
                ▼
STAGE 3: ADD APPROVED PLAYER TO AUCTION
┌──────────────────────────────────────────────────────────────────┐
│ KEY RULE: ONLY APPROVED PLAYERS CAN BE ADDED TO AUCTION         │
│                                                                  │
│ Endpoint: POST /api/players/{id}/add-to-auction                │
│ Required: AddToAuctionRequest (age, city, batting style, etc.)  │
│                                                                  │
│ VALIDATION:                                                      │
│   ✓ Player must exist                                            │
│   ✓ Player must have status = APPROVED                           │
│   ✓ Player must not already be in the auction pool             │
│   ✓ Tournament owner verification                               │
└──────────────────────────────┬───────────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────────┐
│ AUCTION PLAYER CREATED IN AUCTION POOL                          │
├──────────────────────────────────────────────────────────────────┤
│ Entity: AuctionPlayer                                            │
│ - id: Long (new ID, different from Player)                     │
│ - player: Player (reference to registered player)              │
│ - playerNumber: String (copied from Player)                    │
│ - firstName: String (copied from Player)                       │
│ - role: String (copied from Player)                            │
│ - auctionStatus: AuctionStatus.UPCOMING (initially)            │
│ - tournament: Tournament (same as Player's tournament)          │
│ - basePrice: Long (provided during add-to-auction)             │
│ - age, city, battingStyle, bowlingStyle: String (provided)     │
│ - sortOrder: Integer (auction queue position)                  │
│ - photo: byte[] (copied from Player)                           │
│ - createdAt: LocalDateTime (auto-set)                          │
└──────────────────────────────┬───────────────────────────────────┘
                               │
                               ▼
STAGE 4: AUCTION PARTICIPATION
┌──────────────────────────────────────────────────────────────────┐
│ Auction Player is Ready for Bidding                             │
│                                                                  │
│ Possible Transitions:                                            │
│   UPCOMING → SOLD (when team wins the bid)                      │
│   UPCOMING → UNSOLD (if no bid wins)                            │
│   UNSOLD → UPCOMING (when requeued for next auction)            │
└──────────────────────────────────────────────────────────────────┘
```

---

## Key Business Rules Enforced

### Rule 1: Players Must Be Added to Tournament's Players List
**Implementation**: Tournament Entity
```java
@OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
@Builder.Default
@ToString.Exclude
private List<Player> players = new ArrayList<>();
```
- When a Player is created with `tournament_id`, it automatically becomes part of that tournament's players list
- JPA cascade ensures referential integrity

### Rule 2: Only APPROVED Players Can Participate in Auction
**Implementation**: AuctionPlayerService.promoteToAuction()
```java
// Only APPROVED players can enter the auction pool
if (player.getStatus() != PlayerStatus.APPROVED) {
    throw new IllegalArgumentException(
            "Player '" + player.getFirstName() + " " + player.getLastName()
                    + "' is not APPROVED (current status: " + player.getStatus() + "). "
                    + "Approve the player before adding to auction.");
}
```
- This validation happens BEFORE creating the AuctionPlayer record
- Prevents rejected or pending players from entering the auction

### Rule 3: No Duplicate Players in Auction Pool
**Implementation**: AuctionPlayerService.promoteToAuction()
```java
// Prevent duplicate entries in the same tournament's auction pool
if (auctionPlayerRepository.existsByPlayerIdAndTournamentId(playerId, tournament.getId())) {
    throw new IllegalArgumentException(
            "Player '" + player.getFirstName() + " " + player.getLastName()
                    + "' is already in the auction pool for this tournament.");
}
```

---

## API Workflow Steps

### Step 1: Register Player
```bash
POST /api/players/register/{tournamentId}
Content-Type: multipart/form-data

{
  firstName: "John",
  lastName: "Doe",
  role: "Batsman",
  dob: "1990-01-15",
  photo: <file>,
  paymentProof: <file>
}

Response:
{
  id: 1,
  playerNumber: "P001",
  firstName: "John",
  lastName: "Doe",
  role: "Batsman",
  status: "PENDING",
  tournamentId: 10,
  photoUrl: "/api/players/1/photo",
  paymentProofUrl: "/api/players/1/payment-proof"
}
```

### Step 2: Approve Player (Tournament Owner Only)
```bash
PATCH /api/players/{id}/approve
Authorization: Bearer <token>

Response:
{
  id: 1,
  status: "APPROVED"
}
```

### Step 3: Add Approved Player to Auction (Tournament Owner Only)
```bash
POST /api/players/{id}/add-to-auction
Authorization: Bearer <token>
Content-Type: application/json

{
  age: 28,
  city: "Mumbai",
  battingStyle: "Right-handed",
  bowlingStyle: "Right-arm",
  basePrice: 100000
}

Response:
{
  id: 50,
  playerId: 1,
  playerNumber: "P001",
  firstName: "John",
  lastName: "Doe",
  role: "Batsman",
  auctionStatus: "UPCOMING",
  basePrice: 100000,
  age: 28,
  city: "Mumbai",
  battingStyle: "Right-handed",
  bowlingStyle: "Right-arm",
  tournamentId: 10,
  sortOrder: 1,
  photoUrl: "/api/auction-players/50/photo"
}
```

### Step 4: Sell Auction Player (During Auction)
```bash
PATCH /api/auction-players/{id}/sell
Authorization: Bearer <token>
Content-Type: application/json

{
  teamId: 5,
  soldPrice: 150000
}

Response:
{
  id: 50,
  playerId: 1,
  playerNumber: "P001",
  auctionStatus: "SOLD",
  soldToTeamId: 5,
  soldToTeamName: "Team A",
  soldPrice: 150000
}
```

---

## Status Enums

### PlayerStatus
- **PENDING**: Player registered, awaiting tournament owner approval
- **APPROVED**: Tournament owner approved, eligible to be added to auction
- **REJECTED**: Tournament owner rejected, cannot participate in auction

### AuctionStatus
- **UPCOMING**: Player is in auction queue, waiting to be auctioned
- **SOLD**: Player has been sold to a team
- **UNSOLD**: Player did not get sold in the auction round

---

## Data Relationships

### Player → Tournament (Many-to-One)
```
Tournament
  ├── players: List<Player>
  └── auctionPlayers: List<AuctionPlayer>

Player
  ├── tournament: Tournament
  └── status: PlayerStatus (PENDING, APPROVED, REJECTED)

AuctionPlayer
  ├── player: Player (reference to registered player, can be null for admin-created)
  ├── tournament: Tournament
  └── auctionStatus: AuctionStatus (UPCOMING, SOLD, UNSOLD)
```

---

## Database Schema Relationships

```
┌─────────────────┐
│   tournaments   │
│   (primary key: id)
└────────┬────────┘
         │
         │ one-to-many
         │
    ┌────▼──────────┐
    │    players    │ ← Player registration list
    │   (player_id, status, tournament_id)
    └────┬──────────┘
         │
         │ (reference)
         │
    ┌────▼─────────────────┐
    │  auction_players     │ ← Auction pool
    │  (auction_player_id, player_id, tournament_id, auctionStatus)
    └──────────────────────┘
```

---

## Verification Checklist

- [x] Player registration creates record in `players` table with `status = PENDING`
- [x] Only tournament owner can approve/reject players
- [x] Only players with `status = APPROVED` can be promoted to auction
- [x] Promotion creates new record in `auction_players` table
- [x] Original player record remains unchanged during auction
- [x] One player can participate in only one auction per tournament
- [x] AuctionPlayer can be sold, marked unsold, or requeued
- [x] Cascade rules ensure data integrity
- [x] All validations are in place and enforced

---

## Integration Summary

The auction system implements a **two-stage player management approach**:

1. **Registration Stage** (`players` table): Players register and are vetted by tournament owner
2. **Auction Stage** (`auction_players` table): Only approved players are added to the auction pool

This separation allows:
- Multiple tournament registration campaigns
- Fair player vetting before auction
- Flexibility to modify player details before auction inclusion
- Audit trail of who was approved vs rejected
- Easy addition of new players at any point


