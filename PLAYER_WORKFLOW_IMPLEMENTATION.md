# Player Auction Workflow - Implementation Guide

## Quick Summary of the Business Rule

When a Player is added to the system, they should:
1. ✅ Be automatically added to the Tournament's Players list
2. ✅ Start with status = PENDING (awaiting approval)
3. ✅ Only players with status = APPROVED can participate in the Auction

---

## Complete Data Flow

### Phase 1: Player Registration
Players register through public API or are added by tournament owner.

```
POST /api/players/register/{tournamentId}
│
└─→ Creates new Player record
    ├─ playerNumber: Auto-generated (P001, P002, etc.)
    ├─ firstName, lastName, dob, role
    ├─ photo, paymentProof: Optional file uploads
    ├─ status: PENDING (fixed)
    ├─ tournament: References the tournament
    └─ Added to: tournament.players list (automatic via JPA)
```

**Response:**
```json
{
  "id": 1,
  "playerNumber": "P001",
  "firstName": "John",
  "lastName": "Doe",
  "role": "Batsman",
  "status": "PENDING",
  "tournamentId": 10
}
```

---

### Phase 2: Player Approval by Tournament Owner
Tournament owner reviews and approves/rejects players.

```
PATCH /api/players/{id}/approve      ✓ APPROVE
PATCH /api/players/{id}/reject       ✗ REJECT
│
└─→ Player.status changes:
    ├─ PENDING → APPROVED (if approved)
    ├─ PENDING → REJECTED (if rejected)
    └─ Player record remains in tournament.players list
```

**Approve Response:**
```json
{
  "id": 1,
  "status": "APPROVED"
}
```

---

### Phase 3: Only APPROVED Players Enter Auction

**The Key Business Rule is Enforced Here:**

```
POST /api/players/{id}/add-to-auction
│
└─→ AuctionPlayerService.promoteToAuction()
    │
    ├─ Validation 1: Player must exist ✓
    ├─ Validation 2: Player.status must be APPROVED ✓ ← KEY RULE
    ├─ Validation 3: Player not already in auction ✓
    └─ Validation 4: Tournament owner verification ✓
    
    └─→ If all validations pass:
        Creates NEW AuctionPlayer record
        ├─ player_id: Links back to Player
        ├─ First name, playerNumber, role: COPIED from Player
        ├─ age, city, batting style: PROVIDED in request
        ├─ basePrice: PROVIDED in request
        ├─ auctionStatus: UPCOMING
        ├─ tournament: SAME as Player's tournament
        └─ Added to: tournament.auctionPlayers list
```

**Error if Player not APPROVED:**
```json
{
  "error": "Player 'John Doe' is not APPROVED (current status: PENDING). Approve the player before adding to auction."
}
```

**Success Response:**
```json
{
  "id": 50,
  "playerId": 1,
  "playerNumber": "P001",
  "firstName": "John",
  "lastName": "Doe",
  "role": "Batsman",
  "auctionStatus": "UPCOMING",
  "basePrice": 100000,
  "age": 28,
  "city": "Mumbai",
  "battingStyle": "Right-handed",
  "bowlingStyle": "Right-arm",
  "tournamentId": 10,
  "sortOrder": 1
}
```

---

### Phase 4: Auction Participation

Once in auction pool, AuctionPlayer participates in bidding:

```
Auction Player Status Transitions:
├─ UPCOMING (default) → SOLD (when bid wins)
├─ UPCOMING → UNSOLD (if no winning bid)
└─ UNSOLD → UPCOMING (if requeued)
```

**Sell an Auction Player:**
```
PATCH /api/auction-players/{id}/sell
├─ teamId: Team that won the bid
└─ soldPrice: Price paid (must be >= basePrice)
```

---

## Database Schema Relationship

```sql
-- Tournament has Players and AuctionPlayers
tournaments
├─ players (1-to-many)
│  └─ status: PENDING, APPROVED, or REJECTED
└─ auctionPlayers (1-to-many)
   └─ auctionStatus: UPCOMING, SOLD, or UNSOLD

-- Each AuctionPlayer references a Player
auctionPlayers
├─ player_id (FK to players)
└─ Can be NULL (for admin-created players without registration)
```

---

## API Endpoints Summary

### Player Management (Registration & Approval)

| Method | Endpoint | Purpose | Auth |
|--------|----------|---------|------|
| POST | `/api/players/register/{tournamentId}` | Public player registration | No |
| GET | `/api/tournaments/{tournamentId}/players` | List all players (filter by status) | Yes |
| GET | `/api/tournaments/{tournamentId}/players/approved` | **NEW:** List only APPROVED players | Yes |
| GET | `/api/tournaments/{tournamentId}/players/stats` | **NEW:** Get player statistics | Yes |
| GET | `/api/players/{id}` | Get single player details | Yes |
| PUT | `/api/players/{id}` | Update player | Yes |
| DELETE | `/api/players/{id}` | Delete player | Yes |
| PATCH | `/api/players/{id}/approve` | Approve player for auction | Yes |
| PATCH | `/api/players/{id}/reject` | Reject player | Yes |
| POST | `/api/players/{id}/add-to-auction` | **Only APPROVED:** Add to auction pool | Yes |
| GET | `/api/players/{id}/photo` | Download player photo | No |
| GET | `/api/players/{id}/payment-proof` | Download payment proof | Yes |

### Auction Player Management

| Method | Endpoint | Purpose | Auth |
|--------|----------|---------|------|
| GET | `/api/tournaments/{tournamentId}/auction-players` | List auction pool | Yes |
| POST | `/api/tournaments/{tournamentId}/auction-players` | Create auction player directly | Yes |
| GET | `/api/auction-players/{id}` | Get auction player | Yes |
| PUT | `/api/auction-players/{id}` | Update auction player | Yes |
| DELETE | `/api/auction-players/{id}` | Remove from auction pool | Yes |
| PATCH | `/api/auction-players/{id}/sell` | Mark as sold to team | Yes |
| PATCH | `/api/auction-players/{id}/unsold` | Mark as unsold | Yes |
| GET | `/api/auction-players/{id}/photo` | Download auction player photo | No |

---

## Code Implementation Details

### Entity Relationships

**Player.java:**
```java
@Entity
@Table(name = "players")
public class Player {
    @Id
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PlayerStatus status = PlayerStatus.PENDING;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;
    
    // ... other fields
}
```

**AuctionPlayer.java:**
```java
@Entity
@Table(name = "auction_players")
public class AuctionPlayer {
    @Id
    private Long id;
    
    // Reference back to the registered Player
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;  // Can be null for admin-created
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AuctionStatus auctionStatus = AuctionStatus.UPCOMING;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;
    
    // ... other fields
}
```

**Tournament.java:**
```java
@Entity
@Table(name = "tournaments")
public class Tournament {
    @Id
    private Long id;
    
    // Automatically maintains list of all players registered for this tournament
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<Player> players = new ArrayList<>();
    
    // Automatically maintains list of all players in the auction pool
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<AuctionPlayer> auctionPlayers = new ArrayList<>();
    
    // ... other fields
}
```

### Key Validation in AuctionPlayerService

```java
@Transactional
public AuctionPlayerResponse promoteToAuction(Long playerId, AddToAuctionRequest req, User user) {
    Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new ResourceNotFoundException("Player not found"));

    // Ownership verification
    Tournament tournament = tournamentService.findAndVerifyOwner(player.getTournament().getId(), user);

    // ← KEY BUSINESS RULE: Only APPROVED players can enter auction
    if (player.getStatus() != PlayerStatus.APPROVED) {
        throw new IllegalArgumentException(
                "Player '" + player.getFirstName() + " " + player.getLastName()
                        + "' is not APPROVED (current status: " + player.getStatus() + "). "
                        + "Approve the player before adding to auction.");
    }

    // Prevent duplicates
    if (auctionPlayerRepository.existsByPlayerIdAndTournamentId(playerId, tournament.getId())) {
        throw new IllegalArgumentException(
                "Player already in the auction pool for this tournament");
    }

    // Create auction player
    AuctionPlayer ap = AuctionPlayer.builder()
            .player(player)
            .playerNumber(player.getPlayerNumber())
            .firstName(player.getFirstName())
            .lastName(player.getLastName())
            .role(player.getRole())
            .age(req.getAge())
            .city(req.getCity())
            .battingStyle(req.getBattingStyle())
            .bowlingStyle(req.getBowlingStyle())
            .basePrice(req.getBasePrice())
            .photo(player.getPhoto())
            .photoContentType(player.getPhotoContentType())
            .auctionStatus(AuctionStatus.UPCOMING)
            .tournament(tournament)
            .sortOrder(nextOrder)
            .build();

    return toResponse(auctionPlayerRepository.save(ap));
}
```

---

## Status Enums

### PlayerStatus
```java
public enum PlayerStatus {
    PENDING,   // Initial state - awaiting tournament owner approval
    APPROVED,  // Tournament owner approved - eligible for auction
    REJECTED   // Tournament owner rejected - cannot participate
}
```

### AuctionStatus
```java
public enum AuctionStatus {
    UPCOMING,  // Waiting to be auctioned
    SOLD,      // Sold to a team
    UNSOLD     // Did not get sold in auction round
}
```

---

## Workflow Examples

### Example 1: Successful Player Journey

```
1. Player registers
   POST /api/players/register/10
   → Response: id=1, status=PENDING

2. Tournament owner approves
   PATCH /api/players/1/approve
   → Response: status=APPROVED

3. Tournament owner adds to auction
   POST /api/players/1/add-to-auction
   {
     "age": 28,
     "city": "Mumbai",
     "battingStyle": "Right-handed",
     "bowlingStyle": "Right-arm",
     "basePrice": 100000
   }
   → Response: id=50 (AuctionPlayer), auctionStatus=UPCOMING

4. Player is auctioned and sold
   PATCH /api/auction-players/50/sell
   {
     "teamId": 5,
     "soldPrice": 150000
   }
   → Response: auctionStatus=SOLD, soldToTeamId=5, soldPrice=150000
```

### Example 2: Rejected Player

```
1. Player registers
   POST /api/players/register/10
   → Response: id=2, status=PENDING

2. Tournament owner rejects
   PATCH /api/players/2/reject
   → Response: status=REJECTED

3. Cannot add to auction (validation fails)
   POST /api/players/2/add-to-auction
   → Error: "Player is not APPROVED (current status: REJECTED)"
```

### Example 3: Using New Statistics Endpoint

```
GET /api/tournaments/10/players/stats
→ Response:
{
  "totalPlayers": 100,
  "pending": 45,
  "approved": 50,
  "rejected": 5
}
```

---

## Benefits of This Two-Stage Approach

1. **Registration Stage** (Players table)
   - Players register independently
   - Tournament owner can vet players
   - Audit trail of approvals/rejections
   - Can manage multiple registration periods

2. **Auction Stage** (AuctionPlayers table)
   - Only qualified (APPROVED) players participate
   - Flexibility to add additional cricket stats at auction time
   - Can requeue unsold players for next auction
   - Admin can create additional auction players without registration

3. **Data Integrity**
   - One player can only be added to auction once per tournament
   - Original player record preserved for audit
   - Cascade rules prevent orphaned records

---

## Testing Checklist

- [ ] Player can register publicly (no auth required)
- [ ] Player starts with PENDING status
- [ ] Player added to tournament.players list
- [ ] Tournament owner can approve player
- [ ] Tournament owner can reject player
- [ ] Only APPROVED players can be added to auction
- [ ] Adding PENDING player to auction fails
- [ ] Adding REJECTED player to auction fails
- [ ] Duplicate auction entries prevented
- [ ] Approved players list endpoint works
- [ ] Player statistics endpoint shows correct counts
- [ ] AuctionPlayer can be sold, unsold, or requeued
- [ ] Original Player record unchanged after auction sale

---

## Summary

**The implementation is complete and enforces the business rule:**

✅ **When a Player is added → They go to the Players list with status = PENDING**
✅ **Only players with status = APPROVED → Can participate in the Auction**

All validations are in place, and the data flow is enforced at the service layer.


