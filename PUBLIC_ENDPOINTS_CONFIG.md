# Public Endpoints Configuration

## Summary
The following endpoints have been configured to run without authentication:

1. **GET** `/api/tournaments/{tournamentId}/owner-view`
2. **GET** `/api/tournaments/{tournamentId}/auction-players`
3. **GET** `/api/tournaments/{tournamentId}/team-purses`

## Files Modified

### 1. SecurityConfig.java
- Added three new permitAll() rules in the security configuration
- Endpoints pattern matching both with and without `/api/` prefix for context-path compatibility

**Changes:**
```java
// Public tournament data endpoints (owner-view, auction-players, team-purses)
.requestMatchers(HttpMethod.GET, "/tournaments/*/owner-view", "/api/tournaments/*/owner-view").permitAll()
.requestMatchers(HttpMethod.GET, "/tournaments/*/auction-players", "/api/tournaments/*/auction-players").permitAll()
.requestMatchers(HttpMethod.GET, "/tournaments/*/team-purses", "/api/tournaments/*/team-purses").permitAll()
```

### 2. OwnerViewController.java
- Updated `getOwnerView()` method to support optional authentication
- If authentication is not provided, `user` is set to `null`

**Changes:**
```java
@GetMapping("/{tournamentId}/owner-view")
public ResponseEntity<OwnerViewResponse> getOwnerView(
        @PathVariable Long tournamentId, Authentication auth) {
    User user = null;
    if (auth != null && auth.isAuthenticated()) {
        user = authService.getUserByEmail(auth.getName());
    }
    return ResponseEntity.ok(ownerViewService.getOwnerView(tournamentId, user));
}
```

### 3. OwnerViewService.java
- Updated `getOwnerView()` method to skip ownership verification when accessed publicly
- Now uses `findById()` instead of `findAndVerifyOwner()`

**Changes:**
```java
public OwnerViewResponse getOwnerView(Long tournamentId, User user) {
    // For public access, skip ownership verification
    Tournament tournament = tournamentService.findById(tournamentId);
    // ... rest of the method remains the same
}
```

### 4. AuctionPlayerController.java
- Updated `getAllByTournament()` method to support optional authentication
- If authentication is not provided, `user` is set to `null`

**Changes:**
```java
@GetMapping("/tournaments/{tournamentId}/auction-players")
public ResponseEntity<List<AuctionPlayerResponse>> getAllByTournament(
        @PathVariable Long tournamentId, Authentication auth) {
    User user = null;
    if (auth != null && auth.isAuthenticated()) {
        user = currentUser(auth);
    }
    return ResponseEntity.ok(
            auctionPlayerService.getAllByTournament(tournamentId, user));
}
```

### 5. AuctionPlayerService.java
- Updated `getAllByTournament()` method to skip ownership verification when accessed publicly
- Now returns auction players without verifying owner

**Changes:**
```java
public List<AuctionPlayerResponse> getAllByTournament(Long tournamentId, User user) {
    // For public access, skip ownership verification
    return auctionPlayerRepository.findByTournamentIdOrderBySortOrder(tournamentId)
            .stream().map(this::toResponse).toList();
}
```

### 6. TeamPurseController.java
- Removed `@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")` annotation from `getAllTeamPurses()` method
- Endpoint now allows public access

**Changes:**
```java
@GetMapping("/tournaments/{tournamentId}/team-purses")
@Operation(summary = "Get all team purses for a tournament",
        description = "Returns financial details for all teams in a tournament")
// @PreAuthorize removed - now publicly accessible
public ResponseEntity<List<TeamPurseResponse>> getAllTeamPurses(
        @Parameter(description = "Tournament ID") @PathVariable Long tournamentId) {
    List<TeamPurseResponse> purses = teamPurseService.getAllTeamPurses(tournamentId);
    return ResponseEntity.ok(purses);
}
```

## Usage Examples

### Without Authentication (Public)
```bash
curl -X GET https://auctiondeck-api-production.up.railway.app/api/tournaments/1/owner-view

curl -X GET https://auctiondeck-api-production.up.railway.app/api/tournaments/1/auction-players

curl -X GET https://auctiondeck-api-production.up.railway.app/api/tournaments/1/team-purses
```

### With Authentication (Still Supported)
```bash
curl -X GET https://auctiondeck-api-production.up.railway.app/api/tournaments/1/owner-view \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Backward Compatibility
- These endpoints still support authentication if provided
- Authenticated and unauthenticated requests will both work
- No breaking changes to existing integrations

## Notes
- The security filter chain uses wildcard patterns (`/tournaments/*/endpoint`) to match any tournament ID
- Both `/tournaments/` and `/api/tournaments/` patterns are allowed for context-path compatibility
- All other endpoints remain protected and require authentication

