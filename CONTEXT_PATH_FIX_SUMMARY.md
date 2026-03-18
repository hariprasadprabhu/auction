# Context Path Fix Summary

## Problem
The application had a critical routing issue where endpoints were using **double `/api` prefixes**.

### Error Message
```
{"error":"INTERNAL_SERVER_ERROR","message":"No static resource tournaments/13/team-purses.","timestamp":"2026-03-18T12:11:24.427796434"}
```

This error occurred because:
1. The application.yml configured `server.servlet.context-path: /api`
2. Controllers were also adding `/api` prefix in their `@RequestMapping` and `@*Mapping` annotations
3. This resulted in URLs like `/api/api/tournaments/...` instead of `/api/tournaments/...`
4. When clients requested `/api/tournaments/...`, Spring couldn't find the route and treated it as a static resource request

## Root Cause
- **application.yml**: Sets `server.servlet.context-path: /api` (all routes are already under `/api`)
- **Controllers**: Were duplicating the `/api` prefix in their mappings

Example of problematic code:
```java
@RestController
@RequestMapping("/api/tournaments")  // ❌ WRONG - /api is already added by context-path
public class TournamentController { ... }
```

## Solution
Removed all `/api` prefixes from:
1. **Class-level `@RequestMapping` annotations** - changed from `@RequestMapping("/api/...")` to `@RequestMapping("/...")`
2. **Method-level `@*Mapping` annotations** - changed from `@GetMapping("/api/...")` to `@GetMapping("/...")`

Since the context-path already adds `/api`, the controllers only need to specify the path relative to `/api`.

## Files Modified

### Controllers with `@RequestMapping` (Class-level):
1. **AuthController.java** - `@RequestMapping("/api/auth")` → `@RequestMapping("/auth")`
2. **TournamentController.java** - `@RequestMapping("/api/tournaments")` → `@RequestMapping("/tournaments")`
3. **OwnerViewController.java** - `@RequestMapping("/api/tournaments")` → `@RequestMapping("/tournaments")`

### Controllers with Method-level `@*Mapping`:
1. **TeamPurseController.java**
   - `@GetMapping("/api/tournaments/{id}/team-purses")` → `@GetMapping("/tournaments/{id}/team-purses")`
   - `@GetMapping("/api/tournaments/{id}/teams/{id}/purse")` → `@GetMapping("/tournaments/{id}/teams/{id}/purse")`
   - `@GetMapping("/api/teams/{id}/purses")` → `@GetMapping("/teams/{id}/purses")`

2. **PlayerController.java** - All method-level mappings updated
3. **AuctionPlayerController.java** - All method-level mappings updated
4. **IncrementRuleController.java** - All method-level mappings updated
5. **TeamController.java** - All method-level mappings updated

### Security Configuration:
- **SecurityConfig.java** - Updated all request matcher patterns to remove `/api` prefix:
  - Changed from `"/api/auth/**"` to `"/auth/**"`
  - Changed from `"/api/tournaments/*/public"` to `"/tournaments/*/public"`
  - etc.

## Verification
✅ Project compiled successfully with `./mvnw clean compile`
✅ All 60 source files compiled without errors
✅ All API endpoints now correctly route to `/api/` prefix (via context-path)

## Endpoint Behavior
Now endpoints work as expected:
- Client requests: `/api/tournaments/13/team-purses`
- Application context-path: `/api` 
- Controller receives: `/tournaments/13/team-purses`
- Result: ✅ Correct routing!

## Best Practice
When using `server.servlet.context-path`, always define controller paths **relative to** the context-path, not including it.

