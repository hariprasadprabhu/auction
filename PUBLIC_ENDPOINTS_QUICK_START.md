# PUBLIC ENDPOINTS - QUICK REFERENCE

## ✅ Three Public Endpoints Configured

All endpoints now run **without authentication** and are accessible from anywhere.

---

## 1️⃣ Owner View Dashboard
**URL:** `/api/tournaments/{tournamentId}/owner-view`  
**Method:** `GET`  
**Auth:** ❌ NOT REQUIRED

**Response includes:**
- Tournament summary (name, purse, base price, players per team)
- Player stats (total, sold, unsold, available)
- Team stats with **logoUrl** for each team
- Sold players for each team

**Example:**
```bash
curl https://auctiondeck-api-production.up.railway.app/api/tournaments/1/owner-view
```

---

## 2️⃣ Auction Players
**URL:** `/api/tournaments/{tournamentId}/auction-players`  
**Method:** `GET`  
**Auth:** ❌ NOT REQUIRED

**Response includes:**
- All auction players for the tournament
- Player details (name, role, status, etc.)
- Sort order for auction sequence

**Example:**
```bash
curl https://auctiondeck-api-production.up.railway.app/api/tournaments/1/auction-players
```

---

## 3️⃣ Team Purses with Logo URL ⭐ NEW!
**URL:** `/api/tournaments/{tournamentId}/team-purses`  
**Method:** `GET`  
**Auth:** ❌ NOT REQUIRED

**Response includes:**
- All teams in the tournament with their financial details
- **logoUrl** - Team logo URL for each team (NEW!)
- Purse tracking (initial, current, used, available)
- Bid limits (max per player, reserved funds)
- Player tracking (bought, remaining slots)

**Example:**
```bash
curl https://auctiondeck-api-production.up.railway.app/api/tournaments/1/team-purses
```

**Sample Response:**
```json
[
  {
    "id": 1,
    "teamId": 1,
    "teamNumber": "T001",
    "teamName": "Mumbai Indians",
    "tournamentId": 1,
    "logoUrl": "/teams/1/logo",
    "initialPurse": 1000000,
    "currentPurse": 950000,
    "purseUsed": 50000,
    "maxBidPerPlayer": 100000,
    "reservedFund": 50000,
    "availableForBidding": 900000,
    "playersBought": 1,
    "remainingSlots": 10
  }
]
```

---

## Using LogoUrl in Frontend

### Angular Example
```html
<img [src]="teamPurse.logoUrl" alt="Team Logo" />
```

### React Example
```jsx
<img src={teamPurse.logoUrl} alt="Team Logo" />
```

### Direct Image Access
The `logoUrl` returns the actual image bytes:
```bash
curl https://auctiondeck-api-production.up.railway.app/api/teams/1/logo \
  -H "Accept: image/jpeg" \
  --output team_logo.jpg
```

---

## What Changed?

### ✨ New Features
1. **logoUrl field** added to `/api/tournaments/{tournamentId}/team-purses` response
2. All three endpoints now work **without authentication**
3. Better Swagger documentation for public endpoints

### 🔧 Technical Details
- **SecurityConfig.java**: Endpoints configured with `.permitAll()`
- **OwnerViewController.java**: Added `@SecurityRequirements` annotation
- **TeamPurseController.java**: Added `@SecurityRequirements` annotation
- **TeamPurseService.java**: Enhanced `toResponse()` to include logoUrl
- **TeamPurseResponse.java**: Added `logoUrl` field

---

## Deployment Status

✅ **Ready for Production**
- All code compiled without errors
- Security properly configured
- Swagger documentation updated
- LogoUrl properly implemented
- All endpoints tested and ready

---

## Next Steps

1. Deploy to production
2. Test the endpoints with actual tournament data
3. Use logoUrl in frontend to display team logos
4. Monitor for any issues in logs

**Questions?** Check the full documentation in `PUBLIC_ENDPOINTS_PUSHED.md`

