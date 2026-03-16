# Team Purse Database Schema Reference

## Table: `team_purse`

### Purpose
Stores financial metrics and constraints for each team participating in a tournament.

### Schema

```sql
CREATE TABLE team_purse (
    id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
    team_id               BIGINT NOT NULL,
    tournament_id         BIGINT NOT NULL,
    initial_purse         BIGINT NOT NULL,
    current_purse         BIGINT NOT NULL,
    purse_used            BIGINT NOT NULL DEFAULT 0,
    max_bid_per_player    BIGINT NOT NULL,
    reserved_fund         BIGINT NOT NULL,
    available_for_bidding BIGINT NOT NULL,
    players_bought        INT NOT NULL DEFAULT 0,
    remaining_slots       INT NOT NULL,
    created_at            TIMESTAMP NOT NULL,
    updated_at            TIMESTAMP NOT NULL,
    
    FOREIGN KEY (team_id) REFERENCES teams(id),
    FOREIGN KEY (tournament_id) REFERENCES tournaments(id),
    UNIQUE KEY uk_team_tournament (team_id, tournament_id),
    
    INDEX idx_tournament_id (tournament_id),
    INDEX idx_team_id (team_id)
);
```

---

## Column Descriptions

### `id`
- **Type:** BIGINT (AUTO_INCREMENT)
- **Nullable:** NO
- **Primary Key:** YES
- **Description:** Unique identifier for each team purse record

---

### `team_id`
- **Type:** BIGINT
- **Nullable:** NO
- **Foreign Key:** teams(id)
- **Unique Constraint:** With tournament_id
- **Description:** Reference to the team participating in tournament

---

### `tournament_id`
- **Type:** BIGINT
- **Nullable:** NO
- **Foreign Key:** tournaments(id)
- **Unique Constraint:** With team_id
- **Description:** Reference to the tournament

---

### `initial_purse`
- **Type:** BIGINT
- **Nullable:** NO
- **Default:** -
- **Formula:** tournament.purseAmount / tournament.totalTeams
- **Description:** Initial budget allocated to team
- **Example:** 1,000,000 (if tournament purse is 10M for 10 teams)

---

### `current_purse`
- **Type:** BIGINT
- **Nullable:** NO
- **Computed Field:** initial_purse - purse_used
- **Description:** Remaining budget available
- **Update Trigger:** On player sale/unsold

**Example Timeline:**
```
Initial:        1,000,000
After 1st sale: 850,000  (1,000,000 - 150,000)
After 2nd sale: 700,000  (1,000,000 - 300,000)
After unsold:   850,000  (1,000,000 - 150,000) [reverted]
```

---

### `purse_used`
- **Type:** BIGINT
- **Nullable:** NO
- **Default:** 0
- **Description:** Total amount spent on player purchases
- **Increment On:** Player sold
- **Decrement On:** Player marked unsold

**State Changes:**
```
Player sold @150K:   purse_used = 0 + 150,000 = 150,000
Player sold @120K:   purse_used = 150,000 + 120,000 = 270,000
Player unsold @150K: purse_used = 270,000 - 150,000 = 120,000
```

---

### `max_bid_per_player`
- **Type:** BIGINT
- **Nullable:** NO
- **Formula:** ROUND(purse_per_team × 0.25)
- **Description:** Maximum amount team can bid for single player
- **Purpose:** Prevent single large purchase; ensure diversity
- **Example:** If purse_per_team = 1M, max_bid = 250K

---

### `reserved_fund`
- **Type:** BIGINT
- **Nullable:** NO
- **Formula:** (purse_per_team / playersPerTeam) × ⌈playersPerTeam / 3⌉
- **Description:** Fund reserved to complete minimum squad
- **Purpose:** Ensures team can purchase at least minimum players
- **Example:** 
  ```
  Purse per team: 1,000,000
  Players per team: 11
  Cost per player: 1,000,000 / 11 = 90,909
  Min to reserve: ceil(11 / 3) = 4 players
  Reserved: 90,909 × 4 = 363,636
  ```

---

### `available_for_bidding`
- **Type:** BIGINT
- **Nullable:** NO
- **Formula:** MAX(0, current_purse - reserved_fund)
- **Description:** Actual spendable budget (excluding reserved fund)
- **Purpose:** Budget validation during bidding
- **Example:**
  ```
  Current purse: 750,000
  Reserved fund: 363,636
  Available: 750,000 - 363,636 = 386,364
  ```

---

### `players_bought`
- **Type:** INT
- **Nullable:** NO
- **Default:** 0
- **Range:** 0 to playersPerTeam
- **Description:** Count of players purchased
- **Increment On:** Player sale
- **Decrement On:** Player marked unsold

---

### `remaining_slots`
- **Type:** INT
- **Nullable:** NO
- **Formula:** playersPerTeam - players_bought
- **Description:** Number of player slots yet to be filled
- **Purpose:** Track squad completion status

**Example:**
```
Players per team: 11
After 3 purchases: remaining_slots = 11 - 3 = 8
After 1 unsold:    remaining_slots = 11 - 2 = 9
```

---

### `created_at`
- **Type:** TIMESTAMP
- **Nullable:** NO
- **Updatable:** NO
- **Auto-Generated:** YES (@PrePersist)
- **Default:** Current timestamp
- **Description:** Record creation timestamp

---

### `updated_at`
- **Type:** TIMESTAMP
- **Nullable:** NO
- **Updatable:** YES
- **Auto-Updated:** YES (@PreUpdate)
- **Description:** Last modification timestamp

---

## Constraints

### Primary Key
- `id` - Single column primary key

### Unique Constraints
- `(team_id, tournament_id)` - Ensures one purse record per team per tournament

### Foreign Keys
- `team_id` → `teams(id)`
- `tournament_id` → `tournaments(id)`

### Indexes
- `idx_tournament_id` - Optimize queries filtering by tournament
- `idx_team_id` - Optimize queries filtering by team

---

## Relationships

```
Tournament (1) ──── (Many) TeamPurse
                        ↓
Team (1) ──── (Many) TeamPurse
```

**Cardinality:**
- One team can have multiple purse records (one per tournament)
- One tournament can have multiple purse records (one per team)
- One purse record belongs to exactly one team and one tournament

---

## Data Integrity Rules

1. **Unique Constraint:** 
   - Cannot have duplicate (team_id, tournament_id) combinations
   - Prevents multiple purse records for same team in same tournament

2. **Foreign Key Constraint:**
   - team_id must reference existing team
   - tournament_id must reference existing tournament
   - Deletion of team/tournament cascades to purse records

3. **Financial Consistency:**
   - `purse_used ≤ initial_purse` (should always be true)
   - `current_purse = initial_purse - purse_used`
   - `available_for_bidding = current_purse - reserved_fund` (with 0 floor)

4. **Squad Consistency:**
   - `players_bought ≤ playersPerTeam`
   - `remaining_slots = playersPerTeam - players_bought`

---

## Typical Query Patterns

### Get all teams' budgets for a tournament
```sql
SELECT tp.*, t.name, t.teamNumber
FROM team_purse tp
JOIN teams t ON tp.team_id = t.id
WHERE tp.tournament_id = ?
ORDER BY t.teamNumber;
```

### Find teams with available budget
```sql
SELECT tp.*, t.name
FROM team_purse tp
JOIN teams t ON tp.team_id = t.id
WHERE tp.tournament_id = ?
  AND tp.available_for_bidding > 0
ORDER BY tp.available_for_bidding DESC;
```

### Get team's portfolio across tournaments
```sql
SELECT tp.*, tournament.name as tournament_name
FROM team_purse tp
JOIN tournaments tournament ON tp.tournament_id = tournament.id
WHERE tp.team_id = ?
ORDER BY tournament.id DESC;
```

### Find teams near budget exhaustion
```sql
SELECT tp.*, t.name
FROM team_purse tp
JOIN teams t ON tp.team_id = t.id
WHERE tp.tournament_id = ?
  AND tp.available_for_bidding < tp.initial_purse * 0.2
ORDER BY tp.available_for_bidding;
```

---

## Cascading Behavior

### On Team Deletion
- Associated purse records are deleted (CASCADE)
- Triggers: `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)`

### On Tournament Deletion
- Associated purse records are deleted (CASCADE)
- Triggers: `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)`

### On Team Update
- Purse record is NOT automatically affected
- Manual update through `TeamPurseService.recalculateAllTeamPurses()` if needed

### On Tournament Update
- All associated purse records should be recalculated
- Triggers: `TournamentService.update()` calls `TeamPurseService.recalculateAllTeamPurses()`

---

## Migration Strategy

### For New Deployments
- Table is auto-created by JPA/Hibernate on application startup
- No manual migration needed

### For Existing Deployments
```sql
-- If migrating from system without TeamPurse:
INSERT INTO team_purse 
SELECT 
  NULL as id,
  t.id as team_id,
  t.tournament_id,
  tournament.purse_amount / tournament.total_teams as initial_purse,
  tournament.purse_amount / tournament.total_teams as current_purse,
  0 as purse_used,
  ROUND((tournament.purse_amount / tournament.total_teams) * 0.25) as max_bid_per_player,
  ROUND((tournament.purse_amount / tournament.total_teams / 11) * 4) as reserved_fund,
  ROUND((tournament.purse_amount / tournament.total_teams) * 0.65) as available_for_bidding,
  0 as players_bought,
  tournament.players_per_team as remaining_slots,
  NOW() as created_at,
  NOW() as updated_at
FROM teams t
JOIN tournaments tournament ON t.tournament_id = tournament.id;
```

---

## Performance Considerations

### Indexes
- `idx_tournament_id` - For queries filtering by tournament (common)
- `idx_team_id` - For queries filtering by team (common)
- Unique constraint on (team_id, tournament_id) creates implicit index

### Query Optimization
- Use indexed columns in WHERE clauses
- Consider pagination for large tournament results
- Consider caching tournament-wide purse summaries

### Bulk Operations
- Recalculation after tournament update should batch updates
- Consider async recalculation for large number of teams

---

## Sample Data

```sql
-- Sample: Tournament with 3 teams
INSERT INTO team_purse VALUES
(1, 10, 1, 1000000, 750000, 250000, 250000, 363636, 386364, 2, 9, NOW(), NOW()),
(2, 11, 1, 1000000, 1000000, 0, 250000, 363636, 636364, 0, 11, NOW(), NOW()),
(3, 12, 1, 1000000, 550000, 450000, 250000, 363636, 186364, 3, 8, NOW(), NOW());
```

---

## Troubleshooting

### Issue: Available for bidding is 0 but purse_used < initial_purse
- Likely cause: Reserved fund is very high
- Check: playersPerTeam configuration

### Issue: Purse record exists but not showing in queries
- Check: team_id and tournament_id are correct
- Verify: Unique constraint not preventing creation

### Issue: Multiple purse records for same team-tournament
- This shouldn't happen (unique constraint prevents it)
- If found: Data corruption; manual cleanup needed

---

## Related Entities

- **Teams Table** - Base team information
- **Tournaments Table** - Tournament configuration (purseAmount, totalTeams, playersPerTeam)
- **AuctionPlayers Table** - Individual player sales (references sold team)

