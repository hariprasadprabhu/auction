# TeamAllowed Read-Only Implementation

## Overview
Made the `teamAllowed` field read-only to prevent any API from modifying this value. It now can only be read through the API and cannot be changed.

## Changes Made

### 1. Tournament Entity (`src/main/java/com/bid/auction/entity/Tournament.java`)
- Changed `teamAllowed` field to have a **package-level setter** using `@Setter(AccessLevel.PACKAGE)`
- This restricts the setter to be only accessible within the same package
- External code (including API requests) cannot set this value directly
- The field is still readable via the public getter

**Before:**
```java
@Setter
private Integer teamAllowed;
```

**After:**
```java
@Setter(AccessLevel.PACKAGE)
private Integer teamAllowed;
```

### 2. TournamentResponse DTO (`src/main/java/com/bid/auction/dto/response/TournamentResponse.java`)
- Added JavaDoc comment to clarify that `teamAllowed` is a read-only field
- Clients can still read this value from API responses
- Clients cannot include this field in create/update requests

**Added Comment:**
```java
/**
 * Read-only field: Auto-calculated based on tournament configuration.
 * Cannot be modified via API.
 */
private Integer teamAllowed;
```

### 3. TournamentRequest DTO
- No changes needed - `teamAllowed` was never exposed in the request DTO
- This confirms the field was already not intended to be modified through API requests

## Impact Analysis

### What Still Works
- ✅ Clients can **read** `teamAllowed` from GET endpoints
- ✅ `teamAllowed` is properly returned in tournament responses
- ✅ TeamService can still read and validate against `teamAllowed` limit
- ✅ Tournament creation and updates work normally

### What is Now Prevented
- ❌ Clients cannot send `teamAllowed` in create/update requests
- ❌ External code cannot modify `teamAllowed` after creation
- ❌ No API endpoint can change this value

## API Endpoints Affected
All tournament-related endpoints are secure:
- `GET /tournaments` - Can read `teamAllowed`
- `GET /tournaments/{id}` - Can read `teamAllowed`
- `GET /tournaments/{id}/public` - Can read `teamAllowed`
- `POST /tournaments` - Cannot set `teamAllowed`
- `PUT /tournaments/{id}` - Cannot modify `teamAllowed`
- `DELETE /tournaments/{id}` - Not applicable

## Notes
- The field is still stored in the database and can be set programmatically by internal services if needed
- The `@Builder` pattern still works as expected with the `@AllArgsConstructor` 
- Any internal initialization of `teamAllowed` via the builder is still possible within the package

