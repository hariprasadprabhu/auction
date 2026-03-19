# Approve All / Reject All Endpoints Documentation

## Overview
These endpoints allow tournament owners to perform bulk approve or reject operations on all PENDING players in a tournament with a single request.

---

## Endpoints

### 1. Approve All Players
**URL:** `/api/tournaments/{tournamentId}/players/approve-all`  
**METHOD:** `PATCH`  
**AUTH:** Required (Bearer Token)  
**CONTENT-TYPE:** `application/json`

#### Description
Approves ALL pending players in the tournament at once. Changes status from `PENDING` to `APPROVED`.

#### Request Parameters
| Parameter | Type | Location | Required | Description |
|-----------|------|----------|----------|-------------|
| tournamentId | Long | Path | Yes | The tournament ID whose pending players to approve |

#### Request Body
Empty body - no additional data needed.

#### Response (200 OK)
```json
{
  "message": "All pending players approved successfully",
  "approvedCount": 25,
  "status": "SUCCESS"
}
```

#### Error Responses
| Status | Error | Description |
|--------|-------|-------------|
| 404 | Not Found | Tournament not found |
| 403 | Forbidden | User is not the tournament owner |
| 401 | Unauthorized | Missing or invalid authentication token |

#### Example Request
```bash
curl -X PATCH "http://localhost:8080/api/tournaments/10/players/approve-all" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

#### Example Response
```json
{
  "message": "All pending players approved successfully",
  "approvedCount": 45,
  "status": "SUCCESS"
}
```

---

### 2. Reject All Players
**URL:** `/api/tournaments/{tournamentId}/players/reject-all`  
**METHOD:** `PATCH`  
**AUTH:** Required (Bearer Token)  
**CONTENT-TYPE:** `application/json`

#### Description
Rejects ALL pending players in the tournament at once. Changes status from `PENDING` to `REJECTED`.

#### Request Parameters
| Parameter | Type | Location | Required | Description |
|-----------|------|----------|----------|-------------|
| tournamentId | Long | Path | Yes | The tournament ID whose pending players to reject |

#### Request Body
Empty body - no additional data needed.

#### Response (200 OK)
```json
{
  "message": "All pending players rejected successfully",
  "rejectedCount": 25,
  "status": "SUCCESS"
}
```

#### Error Responses
| Status | Error | Description |
|--------|-------|-------------|
| 404 | Not Found | Tournament not found |
| 403 | Forbidden | User is not the tournament owner |
| 401 | Unauthorized | Missing or invalid authentication token |

#### Example Request
```bash
curl -X PATCH "http://localhost:8080/api/tournaments/10/players/reject-all" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

#### Example Response
```json
{
  "message": "All pending players rejected successfully",
  "rejectedCount": 35,
  "status": "SUCCESS"
}
```

---

## Implementation Details

### Service Layer (PlayerService.java)

#### approveAll Method
```java
@Transactional
public Map<String, Object> approveAll(Long tournamentId, User user) {
    // 1. Verify user is the tournament owner
    tournamentService.findAndVerifyOwner(tournamentId, user);
    Tournament tournament = tournamentService.findById(tournamentId);
    
    // 2. Fetch all PENDING players
    List<Player> pendingPlayers = playerRepository.findByTournamentAndStatus(
        tournament,
        PlayerStatus.PENDING
    );
    
    // 3. For each pending player:
    //    - Change status to APPROVED
    //    - Auto-promote to auction pool
    for (Player player : pendingPlayers) {
        player.setStatus(PlayerStatus.APPROVED);
        playerRepository.save(player);
        auctionPlayerService.autoPromoteToAuction(player.getId());
    }
    
    // 4. Return success response with count
    return Map.of(
        "message", "All pending players approved successfully",
        "approvedCount", pendingPlayers.size(),
        "status", "SUCCESS"
    );
}
```

#### rejectAll Method
```java
@Transactional
public Map<String, Object> rejectAll(Long tournamentId, User user) {
    // 1. Verify user is the tournament owner
    tournamentService.findAndVerifyOwner(tournamentId, user);
    Tournament tournament = tournamentService.findById(tournamentId);
    
    // 2. Fetch all PENDING players
    List<Player> pendingPlayers = playerRepository.findByTournamentAndStatus(
        tournament,
        PlayerStatus.PENDING
    );
    
    // 3. For each pending player:
    //    - Change status to REJECTED
    //    - Remove from auction pool if present
    for (Player player : pendingPlayers) {
        player.setStatus(PlayerStatus.REJECTED);
        playerRepository.save(player);
        auctionPlayerService.removeFromAuctionIfPresent(player.getId());
    }
    
    // 4. Return success response with count
    return Map.of(
        "message", "All pending players rejected successfully",
        "rejectedCount", pendingPlayers.size(),
        "status", "SUCCESS"
    );
}
```

### Controller Layer (PlayerController.java)

#### approveAllPlayers Endpoint
```java
@PatchMapping("/tournaments/{tournamentId}/players/approve-all")
public ResponseEntity<Map<String, Object>> approveAllPlayers(
        @PathVariable Long tournamentId,
        Authentication auth) {
    return ResponseEntity.ok(playerService.approveAll(tournamentId, currentUser(auth)));
}
```

#### rejectAllPlayers Endpoint
```java
@PatchMapping("/tournaments/{tournamentId}/players/reject-all")
public ResponseEntity<Map<String, Object>> rejectAllPlayers(
        @PathVariable Long tournamentId,
        Authentication auth) {
    return ResponseEntity.ok(playerService.rejectAll(tournamentId, currentUser(auth)));
}
```

### Repository Layer (PlayerRepository.java)

#### New Query Method
```java
List<Player> findByTournamentAndStatus(Tournament tournament, PlayerStatus status);
```

This method already existed and is used to fetch all PENDING players for bulk operations.

---

## Workflow Integration

### Player Status Progression
```
PENDING → (approveAll) → APPROVED → Auto-added to AuctionPlayer pool
         → (rejectAll) → REJECTED → Removed from AuctionPlayer pool
```

### Transactional Behavior
Both methods are marked with `@Transactional` annotation to ensure:
- All operations complete atomically
- If any error occurs, all changes are rolled back
- Database consistency is maintained

### Authorization
Both methods require:
1. User to be authenticated (Bearer Token)
2. User to be the tournament owner
3. Tournament to exist in the system

If authorization fails, a 403 Forbidden or 404 Not Found response is returned.

---

## Use Cases

### Scenario 1: Tournament Owner Reviews Registrations
```
1. Tournament opens for registration
   → Players register (status = PENDING)

2. Registration deadline passed
   → Tournament owner reviews all registrations

3a. All look good → Approve all at once
    PATCH /api/tournaments/10/players/approve-all
    → All PENDING → APPROVED

3b. Need to review individually
    → Use individual approve endpoint
    PATCH /api/players/{id}/approve
```

### Scenario 2: Reject Batch of Players
```
1. Tournament owner finds issues with batch of players
   → Rejects all at once
   PATCH /api/tournaments/10/players/reject-all
   → All PENDING → REJECTED

2. These players can no longer be added to auction
   → New registrations can then be added
```

### Scenario 3: Mixed Approval Strategy
```
1. Some players approved individually
   PATCH /api/players/1/approve
   PATCH /api/players/2/approve

2. Remaining pending players can be approved all at once
   PATCH /api/tournaments/10/players/approve-all
   → Only PENDING players are approved
```

---

## REST Client Examples

### TypeScript/Angular HttpClient
```typescript
// Approve all players
approveAllPlayers(tournamentId: number): Observable<any> {
  return this.http.patch(
    `/api/tournaments/${tournamentId}/players/approve-all`,
    {}
  );
}

// Reject all players
rejectAllPlayers(tournamentId: number): Observable<any> {
  return this.http.patch(
    `/api/tournaments/${tournamentId}/players/reject-all`,
    {}
  );
}
```

### JavaScript Fetch API
```javascript
// Approve all
fetch(`/api/tournaments/${tournamentId}/players/approve-all`, {
  method: 'PATCH',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  }
})
.then(response => response.json())
.then(data => console.log('Success:', data));

// Reject all
fetch(`/api/tournaments/${tournamentId}/players/reject-all`, {
  method: 'PATCH',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  }
})
.then(response => response.json())
.then(data => console.log('Success:', data));
```

### cURL
```bash
# Approve all
curl -X PATCH "http://localhost:8080/api/tournaments/10/players/approve-all" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"

# Reject all
curl -X PATCH "http://localhost:8080/api/tournaments/10/players/reject-all" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

---

## Related Endpoints

### Individual Player Operations
- **Approve Single Player:** `PATCH /api/players/{id}/approve`
- **Reject Single Player:** `PATCH /api/players/{id}/reject`

### Monitoring and Statistics
- **Get All Players:** `GET /api/tournaments/{tournamentId}/players`
- **Get Player Stats:** `GET /api/tournaments/{tournamentId}/players/stats`
  - Returns: `{totalPlayers, pending, approved, rejected}`
- **Get Approved Players:** `GET /api/tournaments/{tournamentId}/players/approved`

---

## Testing Checklist

- [ ] User not authenticated → 401 Unauthorized
- [ ] User is not tournament owner → 403 Forbidden
- [ ] Tournament does not exist → 404 Not Found
- [ ] No pending players exist → Success with count = 0
- [ ] Multiple pending players → All approved/rejected
- [ ] Verify players moved to/from auction pool
- [ ] Database transaction rollback on error
- [ ] Response format matches documentation

---

## Database Impact

### Tables Modified
1. **Player Table**
   - `status` column updated for all matching rows
   - `updated_at` timestamp updated

2. **AuctionPlayer Table** (if applicable)
   - New rows created during `approveAll` (auto-promotion)
   - Rows may be deleted during `rejectAll` (removal from auction)

### Data Consistency
- Operations are atomic (all or nothing)
- No orphaned records created
- Existing relationships maintained
- Purse calculations updated if needed

---

## Performance Considerations

### Large Tournaments
For tournaments with thousands of pending players:
- Query retrieves all PENDING players (indexed on `status`)
- Each player processed individually in the loop
- Consider pagination or batching if performance becomes an issue

### Optimization Tips
1. Tournament + Status index exists: `findByTournamentAndStatus` is optimized
2. Use statistics endpoint before bulk operations to understand scale
3. Monitor transaction duration for large operations

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-03-19 | Initial implementation of approve-all and reject-all endpoints |



