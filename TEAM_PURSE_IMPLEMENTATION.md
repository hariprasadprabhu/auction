# Team Purse Management System - Implementation Guide

## Overview

A new **Team Purse** module has been added to track and manage financial details for each team in a tournament. This system automatically calculates and updates:

- **Current Purse**: Remaining budget available for bidding
- **Max Bid Per Player**: Maximum amount a team can bid for a single player
- **Reserved Fund**: Fund reserved to ensure minimum squad completion
- **Available for Bidding**: Current purse minus reserved fund
- **Players Bought**: Count of players purchased
- **Remaining Slots**: Count of remaining player slots to fill

---

## New Database Table

### `team_purse`
Stores financial metrics for each team in each tournament.

**Columns:**
```sql
id                    - PRIMARY KEY (auto-generated)
team_id              - FOREIGN KEY to teams table (with unique constraint)
tournament_id        - FOREIGN KEY to tournaments table (with unique constraint)
initial_purse        - Initial purse allocated to the team
current_purse        - Current remaining purse (initial_purse - purse_used)
purse_used           - Total amount spent on players
max_bid_per_player   - Maximum bid allowed for a single player (25% of per-team purse)
reserved_fund        - Fund reserved for minimum squad (33% of players * cost per player)
available_for_bidding - Current purse - reserved fund
players_bought       - Number of players purchased
remaining_slots      - Number of remaining slots to fill
created_at           - Timestamp (auto-generated, not updatable)
updated_at           - Timestamp (auto-generated, updated on changes)

UNIQUE CONSTRAINT: (team_id, tournament_id)
```

---

## New Classes & Components

### 1. Entity: `TeamPurse.java`
**Location:** `com.bid.auction.entity`

Represents the financial state of a team in a tournament.

**Key Fields:**
- `initialPurse`: Total budget allocated
- `currentPurse`: Remaining budget
- `purseUsed`: Total spent
- `maxBidPerPlayer`: 25% of per-team purse
- `reservedFund`: Fund reserved for minimum squad
- `availableForBidding`: Current purse - reserved fund
- `playersBought`: Count of players acquired
- `remainingSlots`: Slots remaining to fill

---

### 2. Repository: `TeamPurseRepository.java`
**Location:** `com.bid.auction.repository`

Database access layer for TeamPurse entity.

**Key Methods:**
```java
Optional<TeamPurse> findByTeamIdAndTournamentId(Long teamId, Long tournamentId)
List<TeamPurse> findByTournamentId(Long tournamentId)
List<TeamPurse> findByTeamId(Long teamId)
List<TeamPurse> findByTournamentIdOrderByTeamNumber(Long tournamentId)
void deleteByTeamIdAndTournamentId(Long teamId, Long tournamentId)
void deleteByTournamentId(Long tournamentId)
```

---

### 3. Service: `TeamPurseService.java`
**Location:** `com.bid.auction.service`

Core business logic for managing team purses.

**Key Methods:**

#### `initializePurse(Team, Tournament): TeamPurse`
Initializes purse when a team is created or added to a tournament.

**Calculation Logic:**
- `pursePerTeam` = `tournament.purseAmount / tournament.totalTeams`
- `maxBidPerPlayer` = `pursePerTeam * 0.25` (25%)
- `minPlayersToReserveFor` = `max(1, playersPerTeam / 3)` (at least 33%)
- `costPerPlayer` = `pursePerTeam / playersPerTeam`
- `reservedFund` = `costPerPlayer * minPlayersToReserveFor`
- `availableForBidding` = `pursePerTeam - reservedFund`

#### `updatePurseOnPlayerSold(Team, Tournament, Long soldPrice): TeamPurse`
Called when a player is sold to a team.

**Updates:**
- `purseUsed += soldPrice`
- `currentPurse = initialPurse - purseUsed`
- `playersBought++`
- `remainingSlots--`
- `availableForBidding = max(0, currentPurse - reservedFund)`

#### `updatePurseOnPlayerUnsold(Team, Tournament, Long unsoldPrice): TeamPurse`
Called when a sold player is marked unsold or removed.

**Reverts:**
- `purseUsed -= unsoldPrice`
- `currentPurse = initialPurse - purseUsed`
- `playersBought--`
- `remainingSlots++`
- `availableForBidding = max(0, currentPurse - reservedFund)`

#### `recalculateAllTeamPurses(Tournament): void`
Recalculates all team purses for a tournament when tournament details change.

**Triggered On:**
- Tournament purse amount changes
- Total teams count changes
- Players per team count changes

**Preserves:** Current spending and player counts, recalculates only allocation metrics

#### `getPurse(Long teamId, Long tournamentId): TeamPurseResponse`
Retrieves team purse details for a specific tournament.

#### `getAllTeamPurses(Long tournamentId): List<TeamPurseResponse>`
Retrieves all team purses for a tournament, ordered by team number.

#### `getTeamPurseAcrossTournaments(Long teamId): List<TeamPurseResponse>`
Retrieves all team purses for a team across all tournaments.

---

### 4. DTO: `TeamPurseResponse.java`
**Location:** `com.bid.auction.dto.response`

Response DTO containing all team purse information.

**Fields:**
```java
id                    - Purse record ID
teamId               - Team ID
teamNumber           - Team number (e.g., "T001")
teamName             - Team name
tournamentId         - Tournament ID
initialPurse         - Initial allocation
currentPurse         - Current remaining
purseUsed            - Total spent
maxBidPerPlayer      - Max single bid
reservedFund         - Reserved for minimum squad
availableForBidding  - Available after reservation
playersBought        - Count of players bought
remainingSlots       - Remaining player slots
```

---

### 5. Controller: `TeamPurseController.java`
**Location:** `com.bid.auction.controller`

REST API endpoints for team purse information.

#### Endpoints:

**GET** `/api/tournaments/{tournamentId}/team-purses`
- Returns all team purses for a tournament
- Returns: `List<TeamPurseResponse>`

**GET** `/api/tournaments/{tournamentId}/teams/{teamId}/purse`
- Returns team purse for a specific tournament
- Returns: `TeamPurseResponse`

**GET** `/api/teams/{teamId}/purses`
- Returns all team purses across tournaments
- Returns: `List<TeamPurseResponse>`

---

## Integration Points

### 1. TeamService Updates

**In `create()` method:**
```java
// After saving team
Team savedTeam = teamRepository.save(team);
teamPurseService.initializePurse(savedTeam, tournament);
```

**Trigger:** When a new team is created in a tournament

---

### 2. AuctionPlayerService Updates

**In `sell()` method:**
```java
// After marking player as sold
teamPurseService.updatePurseOnPlayerSold(team, tournament, req.getSoldPrice());
```

**Trigger:** When a player is sold to a team

**In `markUnsold()` method:**
```java
// If player was previously sold
if (ap.getSoldToTeam() != null && ap.getSoldPrice() != null) {
    teamPurseService.updatePurseOnPlayerUnsold(
        ap.getSoldToTeam(), 
        ap.getTournament(), 
        ap.getSoldPrice()
    );
}
```

**Trigger:** When a sold player is marked unsold

---

### 3. TournamentService Updates

**In `update()` method:**
```java
// After saving tournament changes
Tournament updatedTournament = tournamentRepository.save(t);
teamPurseService.recalculateAllTeamPurses(updatedTournament);
```

**Trigger:** When tournament financial details are modified (purse amount, total teams, players per team)

---

## Calculation Examples

### Example Scenario:
```
Tournament Setup:
- Total Purse: 10,000,000
- Total Teams: 10
- Players Per Team: 11
- Base Price: 50,000

Calculations:
- Purse Per Team = 10,000,000 / 10 = 1,000,000
- Max Bid Per Player = 1,000,000 * 0.25 = 250,000
- Min Players to Reserve = max(1, 11/3) = 4 players
- Cost Per Player = 1,000,000 / 11 = 90,909
- Reserved Fund = 90,909 * 4 = 363,636
- Available for Bidding = 1,000,000 - 363,636 = 636,364
```

### Team Purchase Scenario:
```
Initial State:
- Current Purse: 1,000,000
- Purse Used: 0
- Players Bought: 0
- Remaining Slots: 11
- Available for Bidding: 636,364

After buying 3 players @ 150,000 each:
- Purse Used: 450,000
- Current Purse: 550,000
- Players Bought: 3
- Remaining Slots: 8
- Available for Bidding: 186,364 (550,000 - 363,636)
```

---

## Workflow

1. **Tournament Created**
   - Tournament financial parameters set (purse amount, teams, players per team)

2. **Team Created**
   - `TeamService.create()` → `TeamPurseService.initializePurse()`
   - TeamPurse record created with initial calculations

3. **Player Sold**
   - `AuctionPlayerService.sell()` → `TeamPurseService.updatePurseOnPlayerSold()`
   - Team purse updated: spent increases, current/available decrease

4. **Player Unsold**
   - `AuctionPlayerService.markUnsold()` → `TeamPurseService.updatePurseOnPlayerUnsold()`
   - Team purse updated: spent decreases, current/available increase

5. **Tournament Updated**
   - `TournamentService.update()` → `TeamPurseService.recalculateAllTeamPurses()`
   - All team purses recalculated with new tournament metrics

---

## API Usage Examples

### Get All Team Purses for a Tournament
```bash
GET /api/tournaments/1/team-purses
Authorization: Bearer {token}
```

**Response:**
```json
[
  {
    "id": 1,
    "teamId": 10,
    "teamNumber": "T001",
    "teamName": "Mumbai Indians",
    "tournamentId": 1,
    "initialPurse": 1000000,
    "currentPurse": 550000,
    "purseUsed": 450000,
    "maxBidPerPlayer": 250000,
    "reservedFund": 363636,
    "availableForBidding": 186364,
    "playersBought": 3,
    "remainingSlots": 8
  },
  ...
]
```

### Get Specific Team Purse
```bash
GET /api/tournaments/1/teams/10/purse
Authorization: Bearer {token}
```

### Get Team Purses Across All Tournaments
```bash
GET /api/teams/10/purses
Authorization: Bearer {token}
```

---

## Key Features

✅ **Automatic Initialization** - Purse created automatically when team joins tournament

✅ **Real-time Updates** - Purse values update on every player transaction

✅ **Smart Calculations** - Max bid and reserved fund based on tournament metrics

✅ **Budget Enforcement** - Available for bidding ensures minimum squad can be filled

✅ **Transaction Safety** - All updates are transactional with @Transactional annotation

✅ **Audit Trail** - Created/Updated timestamps track changes

✅ **Dynamic Recalculation** - Updates when tournament financial parameters change

✅ **Multi-Tournament Support** - Track purses across multiple tournaments per team

---

## Notes for UI Integration

1. **Display Purse Information:**
   - Show current purse balance during auction
   - Display max bid per player as upper limit hint
   - Show reserved fund to explain unavailable balance
   - Display available for bidding as the "effective" budget

2. **Validation:**
   - UI can fetch team purse before allowing bid placement
   - Compare bid amount against maxBidPerPlayer
   - Compare bid amount against availableForBidding

3. **Real-time Updates:**
   - After player sold/unsold, fetch updated purse data
   - Display updated balance to user immediately

4. **Analytics:**
   - Use purse data to show team budget utilization
   - Display spending breakdown across purchases
   - Show remaining budget visualization

---

## Database Migration

When deploying:
1. The `team_purse` table will be auto-created by Spring Data JPA/Hibernate
2. No manual migration needed if using automatic schema creation
3. Consider pre-populating existing teams' purses if migrating from existing data

---

## Troubleshooting

**Purse not found for team-tournament combination:**
- Ensure team was created through `TeamService.create()` (initializes purse)
- Check that team and tournament IDs are correct

**Available for bidding is negative:**
- This is prevented by `Math.max(0L, ...)` in calculations
- Indicates team is near budget exhaustion

**Purse not updating on player sale:**
- Verify `@Transactional` is present on `sell()` and `markUnsold()` methods
- Check that `TeamPurseService` is injected correctly

---

## Files Modified/Created

**New Files:**
- `/entity/TeamPurse.java`
- `/repository/TeamPurseRepository.java`
- `/service/TeamPurseService.java`
- `/dto/response/TeamPurseResponse.java`
- `/controller/TeamPurseController.java`

**Modified Files:**
- `/service/TeamService.java` - Added TeamPurseService integration
- `/service/AuctionPlayerService.java` - Added purse updates on sell/unsold
- `/service/TournamentService.java` - Added purse recalculation on update

---

## Summary

The Team Purse system provides comprehensive financial tracking for auction teams, ensuring budget compliance and fair competition. All updates are automatic and integrated into the existing auction workflow.

